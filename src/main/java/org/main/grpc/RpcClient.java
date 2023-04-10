package org.main.grpc;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jetbrains.annotations.NotNull;
import org.main.entity.Block;
import org.main.grpc.entity.GetBlockChainResponse;
import org.main.grpc.entity.HeartbeatResponse;
import org.main.grpc.entity.MinedBlockResponse;
import org.main.grpc.entity.MinedBlockResponseCode;
import org.main.java.grpc.BlockOuterClass;
import org.main.java.grpc.BlockServiceGrpc;

import java.util.*;
import java.util.stream.Collectors;

public class RpcClient {


    private final String nodeName;

    private final List<BlockServiceGrpc.BlockServiceBlockingStub> stubs;

    public RpcClient(String nodeName, Properties properties) {
        this.nodeName = nodeName;
        this.stubs = new ArrayList<>();
        init(properties);
    }

    private void init(Properties properties) {
        int nodesAmount = Integer.parseInt(properties.getProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY));
        for (int i = 0; i < nodesAmount; i++) {
            String curNodeName = RpcConfiguration.NODE_PREFIX_PROPERTY + i;
            if (curNodeName.equalsIgnoreCase(nodeName)) {
                continue; // skipping current node
            }
            String target = properties.getProperty(curNodeName);
            String[] targetAddress = target.split(":");
            ManagedChannel channel = ManagedChannelBuilder.forAddress(targetAddress[0], Integer.parseInt(targetAddress[1]))
                    .usePlaintext()
                    .build();
            stubs.add(BlockServiceGrpc.newBlockingStub(channel));
            System.out.printf("Added node to cluster: index: [%d]; address: [%s]%n", i, target);
        }
    }

    public List<MinedBlockResponse> sendBlockBroadcast(Block block) {
        BlockOuterClass.Block rpcBlock = RpcEntityConverter.from(block);
        BlockOuterClass.MinedBlockRequest request = BlockOuterClass.MinedBlockRequest.newBuilder()
                .setBlock(rpcBlock)
                .build();
        return stubs.parallelStream()
                .map(stub -> {
                    String host = stub.getChannel().authority();
                    System.out.printf("Sending block with index: [%d] to node: [%s]%n", block.getIndex(), host);
                    BlockOuterClass.MinedBlockResponse rpcResponse;
                    try {
                        rpcResponse = stub.sendBlock(request);
                    } catch (Exception e) {
                        System.out.printf("Failed to send block to node: [%s]%n", host);
                        return new MinedBlockResponse(host, MinedBlockResponseCode.FAILED, null);
                    }
                    return RpcEntityConverter.from(host, rpcResponse);
                })
                .toList();
    }

    @NotNull
    public GetBlockChainResponse getBlockchain() {
        Map<BlockServiceGrpc.BlockServiceBlockingStub, HeartbeatResponse> heartbeatResponses = sendHeartbeatBroadcast();
        Map.Entry<BlockServiceGrpc.BlockServiceBlockingStub, HeartbeatResponse> hostWithMaxChainLength = heartbeatResponses.entrySet().stream()
                .max(Comparator.comparingLong(host -> host.getValue().chainLength()))
                .filter(heartbeatResponse -> heartbeatResponse.getValue().chainLength() > 0)
                .orElse(null);
        if (hostWithMaxChainLength == null) {
            System.out.println("Can't find host with max blockchain length");
            return GetBlockChainResponse.EMPTY;
        }
        BlockServiceGrpc.BlockServiceBlockingStub stub = hostWithMaxChainLength.getKey();
        BlockOuterClass.GetBlockChainResponse blockchainResponse;
        try {
            blockchainResponse = stub.getBlockchain(Empty.newBuilder().build());
        } catch (Exception  e) {
            System.out.printf("Failed to get blockchain from node: [%s]%n", stub.getChannel().authority());
            return GetBlockChainResponse.EMPTY;
        }
        return RpcEntityConverter.from(blockchainResponse);
    }

    @NotNull
    public Map<BlockServiceGrpc.BlockServiceBlockingStub, HeartbeatResponse> sendHeartbeatBroadcast() {
        return stubs.parallelStream()
                .collect(Collectors.toMap(stub -> stub, stub -> {
                    String host = stub.getChannel().authority();
                    System.out.printf("Sending heartbeat to node: [%s]%n", host);
                    BlockOuterClass.HeartbeatResponse rpcResponse;
                    try {
                        rpcResponse = stub.sendHeartbeat(Empty.newBuilder().build());
                    } catch (Exception e) {
                        System.out.printf("Failed to send heartbeat to node: [%s]%n", host);
                        return new HeartbeatResponse(-1L);
                    }
                    return RpcEntityConverter.from(rpcResponse);
                }));
    }


}
