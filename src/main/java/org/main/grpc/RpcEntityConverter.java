package org.main.grpc;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.main.entity.Block;
import org.main.grpc.entity.MinedBlockResponse;
import org.main.grpc.entity.MinedBlockResponseCode;
import org.main.java.grpc.BlockOuterClass;


public final class RpcEntityConverter {

    private RpcEntityConverter() {}

    @NotNull
    public static MinedBlockResponse from(@NotNull String host, @NotNull BlockOuterClass.MinedBlockResponse rpcResponse) {
        Validate.notNull(host);
        Validate.notNull(rpcResponse);
        return new MinedBlockResponse(host, from(rpcResponse.getCode()), from(rpcResponse.getBlock()));
    }

    @NotNull
    public static MinedBlockResponseCode from(@NotNull BlockOuterClass.ResponseCode rpcResponseCode) {
        Validate.notNull(rpcResponseCode);
        return MinedBlockResponseCode.from(rpcResponseCode.getNumber());
    }

    @NotNull
    public static BlockOuterClass.Block from(@NotNull Block block) {
        Validate.notNull(block);
        return BlockOuterClass.Block.newBuilder()
                .setIndex(block.getIndex())
                .setPrevHash(block.getPrevHash())
                .setHash(block.getHash())
                .setNonce(block.getNonce())
                .setData(block.getData())
                .build();
    }

    @NotNull
    public static Block from(@NotNull BlockOuterClass.Block block) {
        Validate.notNull(block);
        return new Block(block.getIndex(),
                block.getPrevHash(),
                block.getHash(),
                block.getNonce(),
                block.getData());
    }

}
