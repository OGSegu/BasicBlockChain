package org.main.grpc;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.main.entity.Block;
import org.main.grpc.entity.MinedBlockRequest;
import org.main.java.grpc.BlockOuterClass;
import org.main.state.BlockChainService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RpcBlockServiceTest {

    private RpcBlockService rpcBlockService;
    private BlockChainService blockChainService;

    @BeforeEach
    void setUp() {
        blockChainService = mock(BlockChainService.class);
        rpcBlockService = new RpcBlockService(blockChainService);
    }

    @Test
    @DisplayName(
            "Should return an empty list when the specified index is greater than the blockchain size")
    void getBlockchainWhenIndexIsGreaterThanBlockchainSize() {
        int fromIndex = 10;
        List<Block> emptyBlockList = Collections.emptyList();
        when(blockChainService.getBlockChain(fromIndex)).thenReturn(emptyBlockList);

        BlockOuterClass.GetBlockChainRequest request =
                BlockOuterClass.GetBlockChainRequest.newBuilder().setFromIndex(fromIndex).build();
        StreamObserver<BlockOuterClass.GetBlockChainResponse> responseObserver =
                mock(StreamObserver.class);

        rpcBlockService.getBlockchain(request, responseObserver);

        verify(blockChainService, times(1)).getBlockChain(fromIndex);
        verify(responseObserver, times(1))
                .onNext(argThat(response -> response.getBlockList().isEmpty()));
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    @DisplayName("Should return the entire blockchain when the specified index is zero")
    void getBlockchainWhenIndexIsZero() {
        BlockOuterClass.GetBlockChainRequest request =
                BlockOuterClass.GetBlockChainRequest.newBuilder().setFromIndex(0).build();
        StreamObserver<BlockOuterClass.GetBlockChainResponse> responseObserver =
                mock(StreamObserver.class);

        List<Block> blockChain =
                List.of(
                        new Block(0, "stub", "hash1", 0L, "data1"),
                        new Block(1, "hash1", "hash2", 0L, "data2"),
                        new Block(2, "hash2", "hash3", 0L, "data3"));

        when(blockChainService.getBlockChain(0)).thenReturn(blockChain);

        rpcBlockService.getBlockchain(request, responseObserver);

        verify(blockChainService).getBlockChain(0);

        List<BlockOuterClass.Block> rpcBlocks =
                blockChain.stream().map(RpcEntityConverter::from).toList();
        BlockOuterClass.GetBlockChainResponse expectedResponse =
                BlockOuterClass.GetBlockChainResponse.newBuilder().addAllBlock(rpcBlocks).build();

        verify(responseObserver).onNext(eq(expectedResponse));
        verify(responseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should return the blockchain from the specified index")
    void getBlockchainFromSpecifiedIndex() {
        int fromIndex = 1;
        List<Block> expectedBlocks =
                List.of(
                        new Block(1, "prevHash1", "hash1", 1L, "data1"),
                        new Block(2, "prevHash2", "hash2", 2L, "data2"));

        when(blockChainService.getBlockChain(fromIndex)).thenReturn(expectedBlocks);

        BlockOuterClass.GetBlockChainRequest request =
                BlockOuterClass.GetBlockChainRequest.newBuilder().setFromIndex(fromIndex).build();

        StreamObserver<BlockOuterClass.GetBlockChainResponse> responseObserver =
                mock(StreamObserver.class);

        rpcBlockService.getBlockchain(request, responseObserver);

        BlockOuterClass.Block expectedRpcBlock1 =
                BlockOuterClass.Block.newBuilder()
                        .setIndex(1)
                        .setPrevHash("prevHash1")
                        .setHash("hash1")
                        .setNonce(1)
                        .setData("data1")
                        .build();

        BlockOuterClass.Block expectedRpcBlock2 =
                BlockOuterClass.Block.newBuilder()
                        .setIndex(2)
                        .setPrevHash("prevHash2")
                        .setHash("hash2")
                        .setNonce(2)
                        .setData("data2")
                        .build();

        BlockOuterClass.GetBlockChainResponse expectedResponse =
                BlockOuterClass.GetBlockChainResponse.newBuilder()
                        .addAllBlock(List.of(expectedRpcBlock1, expectedRpcBlock2))
                        .build();

        verify(responseObserver).onNext(eq(expectedResponse));
        verify(responseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should reject the block when it is invalid")
    void sendBlockWhenBlockIsInvalidThenReject() {
        Block block = new Block(1, "prevHash", "hash", 123L, "data");
        BlockOuterClass.MinedBlockRequest rpcRequest =
                BlockOuterClass.MinedBlockRequest.newBuilder()
                        .setBlock(RpcEntityConverter.from(block))
                        .build();
        StreamObserver<BlockOuterClass.MinedBlockResponse> responseObserver =
                mock(StreamObserver.class);

        when(blockChainService.onBlockRequestReceived(any(MinedBlockRequest.class)))
                .thenReturn(false);

        rpcBlockService.sendBlock(rpcRequest, responseObserver);

        verify(blockChainService).onBlockRequestReceived(any(MinedBlockRequest.class));
        verify(responseObserver)
                .onNext(
                        argThat(
                                response ->
                                        response.getCode()
                                                == BlockOuterClass.ResponseCode.REJECTED));
        verify(responseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should accept the block when it is valid")
    void sendBlockWhenBlockIsValidThenAccept() {
        Block block = new Block(1, "prevHash", "hash", 123L, "data");
        BlockOuterClass.Block rpcBlock = RpcEntityConverter.from(block);
        BlockOuterClass.MinedBlockRequest rpcRequest =
                BlockOuterClass.MinedBlockRequest.newBuilder().setBlock(rpcBlock).build();
        StreamObserver<BlockOuterClass.MinedBlockResponse> responseObserver =
                mock(StreamObserver.class);

        when(blockChainService.onBlockRequestReceived(any(MinedBlockRequest.class)))
                .thenReturn(true);

        rpcBlockService.sendBlock(rpcRequest, responseObserver);

        verify(blockChainService).onBlockRequestReceived(any(MinedBlockRequest.class));
        verify(responseObserver)
                .onNext(
                        argThat(
                                response ->
                                        response.getCode()
                                                == BlockOuterClass.ResponseCode.ACCEPTED));
        verify(responseObserver).onCompleted();
    }
}