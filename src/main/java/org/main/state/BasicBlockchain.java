package org.main.state;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.main.BlockGenerationUtils;
import org.main.entity.Block;
import org.main.exception.ChainValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicBlockchain {

    protected final List<Block> blocks;
    private final ExecutorService worker;

    public BasicBlockchain(boolean generateGenesis) {
        this.blocks = new ArrayList<>();
        this.worker = Executors.newSingleThreadExecutor();

        if (generateGenesis) {
            this.blocks.add(generateGenesis());
        }
    }

    public void start() {
        worker.submit(new BlockChainMainWorker());
    }

    @VisibleForTesting
    void add(Block block) throws ChainValidationException {
        validateChains();
        blocks.add(block);
    }

    @VisibleForTesting
    void validateChains() throws ChainValidationException {
        if (blocks.isEmpty() || blocks.size() == 1) {
            return;
        }
        for (int i = 0; i < blocks.size() - 1; i++) {
            Block prevBlock = blocks.get(i);
            Block curBlock = blocks.get(i + 1);
            if (!Objects.equals(prevBlock.getHash(), curBlock.getPrevHash())) {
                throw new ChainValidationException(prevBlock.getIndex(), curBlock.getIndex());
            }
        }
    }
    @NotNull
    protected Block getLastBlock()  {
        return blocks.get(blocks.size() - 1);
    }
    private static Block generateGenesis() {
        return new Block(0L, null, BlockGenerationUtils.generateRandomData());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private class BlockChainMainWorker implements Runnable {

        private static final long DELAY = 3000L;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Block lastBlock = getLastBlock();
                Block generatedBlock = new Block(
                        lastBlock.getIndex() + 1, lastBlock.getHash(), BlockGenerationUtils.generateRandomData()
                );
                try {
                    add(generatedBlock);
                    Thread.sleep(DELAY);
                } catch (ChainValidationException e) {
                    System.err.println("Chain validation failed: " + e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
