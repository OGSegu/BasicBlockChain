package org.main.grpc.entity;

import org.main.entity.Block;

import java.util.Collections;
import java.util.List;

public record GetBlockChainResponse(List<Block> blockChain) {

    public static final GetBlockChainResponse EMPTY = new GetBlockChainResponse(Collections.emptyList());

}
