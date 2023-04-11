package org.main.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.main.grpc.entity.MinedBlockRequest;
import org.main.java.grpc.BlockOuterClass;
import org.main.java.grpc.BlockServiceGrpc;
import org.main.state.BlockChainService;

import java.util.List;

public class RpcBlockService extends BlockServiceGrpc.BlockServiceImplBase {

    private final BlockChainService blockChainService;

    public RpcBlockService(BlockChainService blockChainService) {
        this.blockChainService = blockChainService;
    }

    @Override
    public void sendBlock(BlockOuterClass.MinedBlockRequest rpcRequest, StreamObserver<BlockOuterClass.MinedBlockResponse> responseObserver) {
        MinedBlockRequest request = new MinedBlockRequest(RpcEntityConverter.from(rpcRequest.getBlock()));
        System.out.println("Received block request with index: [" + request.block().getIndex() + "]");
        BlockOuterClass.MinedBlockResponse.Builder responseBuilder = BlockOuterClass.MinedBlockResponse.newBuilder();

        boolean result = blockChainService.onBlockRequestReceived(request);

        if (result) {
            responseBuilder.setCode(BlockOuterClass.ResponseCode.ACCEPTED);
        } else {
            responseBuilder.setCode(BlockOuterClass.ResponseCode.REJECTED);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendHeartbeat(Empty request, StreamObserver<BlockOuterClass.HeartbeatResponse> responseObserver) {
        BlockOuterClass.HeartbeatResponse response = BlockOuterClass.HeartbeatResponse.newBuilder()
                .setChainLength(blockChainService.chainSize())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getBlockchain(BlockOuterClass.GetBlockChainRequest request, StreamObserver<BlockOuterClass.GetBlockChainResponse> responseObserver) {
        List<BlockOuterClass.Block> rpcBlocks = blockChainService.getBlockChain(request.getFromIndex()).stream()
                .map(RpcEntityConverter::from)
                .toList();
        BlockOuterClass.GetBlockChainResponse response = BlockOuterClass.GetBlockChainResponse.newBuilder()
                .addAllBlock(rpcBlocks)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
