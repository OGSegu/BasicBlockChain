package org.main.grpc.entity;

import org.main.entity.Block;

public class MinedBlockRequest {

    private final Block block;

    public MinedBlockRequest(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
