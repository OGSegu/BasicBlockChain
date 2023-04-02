package org.main.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.main.entity.Block;
import org.main.grpc.entity.MinedBlockResponse;
import org.main.java.grpc.BlockOuterClass;
import org.main.java.grpc.BlockServiceGrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RpcClient {

    private final List<BlockServiceGrpc.BlockServiceBlockingStub> stubs;

    public RpcClient(Properties properties) {
        this.stubs = new ArrayList<>();
        init(properties);
    }

    private void init(Properties properties) {
        int nodesAmount = Integer.parseInt(properties.getProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY));
        for (int i = 0; i < nodesAmount; i++) {
            String target = properties.getProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + i);
            String[] targetAddress = target.split(":");
            ManagedChannel channel = ManagedChannelBuilder.forAddress(targetAddress[0], Integer.parseInt(targetAddress[1]))
                    .usePlaintext()
                    .build();
            // is a node alive ?
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
                    System.out.printf("Sending block to node with address: [%s]%n", host);
                    BlockOuterClass.MinedBlockResponse rpcResponse = stub.sendBlock(request);
                    return RpcEntityConverter.from(host, rpcResponse);
                })
                .toList();
    }
}
