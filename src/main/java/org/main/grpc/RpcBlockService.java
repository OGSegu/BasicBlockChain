package org.main.grpc;

import io.grpc.stub.StreamObserver;
import org.main.exception.ChainValidationException;
import org.main.grpc.entity.MinedBlockRequest;
import org.main.java.grpc.BlockOuterClass;
import org.main.java.grpc.BlockServiceGrpc;
import org.main.state.BlockChain;

public class RpcBlockService extends BlockServiceGrpc.BlockServiceImplBase {

    private final BlockChain blockchain;

    public RpcBlockService(BlockChain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public void sendBlock(BlockOuterClass.MinedBlockRequest rpcRequest, StreamObserver<BlockOuterClass.MinedBlockResponse> responseObserver) {
        System.out.println("Received request: [" + rpcRequest + "]");

        MinedBlockRequest request = new MinedBlockRequest(RpcEntityConverter.from(rpcRequest.getBlock()));
        BlockOuterClass.MinedBlockResponse.Builder responseBuilder = BlockOuterClass.MinedBlockResponse.newBuilder();
        try {
            blockchain.onBlockRequestReceived(request);
            responseBuilder.setCode(BlockOuterClass.ResponseCode.ACCEPTED);
        } catch (ChainValidationException e) {
            System.out.println("Failed to validate blocks");
            responseBuilder.setCode(BlockOuterClass.ResponseCode.REJECTED);
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
