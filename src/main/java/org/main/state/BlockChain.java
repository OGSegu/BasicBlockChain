package org.main.state;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.main.BlockGenerationUtils;
import org.main.entity.Block;
import org.main.exception.ChainValidationException;
import org.main.grpc.RpcClient;
import org.main.grpc.entity.MinedBlockRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class BlockChain {

    /**
     * Обязательно использовать под {@link BlockChain#lock}
     */
    protected final List<Block> blocks;
    private final ExecutorService mainWorker;
    private final RpcClient rpcClient;

    private final ReentrantLock lock = new ReentrantLock(true);

    public BlockChain(boolean generateGenesis, RpcClient rpcClient) throws InterruptedException {
        this.blocks = generateGenesis ? Arrays.asList(BlockGenerationUtils.generateGenesis()) : new ArrayList<>();
        this.mainWorker = Executors.newSingleThreadExecutor();
        this.rpcClient = rpcClient;
    }
    public void start() {
        mainWorker.submit(new BlockChainMainWorker());
        System.out.println("Blockchain started...");
    }

    public void onBlockRequestReceived(MinedBlockRequest request) throws ChainValidationException {
        lock.lock();
        try {
            Block block = request.getBlock();
            long blockIndex = block.getIndex();
            if (blocks.get((int) blockIndex) != null) {
                System.out.printf("Block with index [%d] is already mined. Skipping...%n", blockIndex);
                // block is already mined. Skipping
                return;
            }
            add(block);
        } finally {
            lock.unlock();
        }
    }

    @VisibleForTesting
    void add(Block newBlock) throws ChainValidationException {
        blocks.add(newBlock);
        try {
            validateChains();
        } catch (ChainValidationException e) {
            blocks.remove(blocks.size() - 1); // добавленный блок "сломал" цепочку
            throw new ChainValidationException(e.getMessage());
        }
    }

    /**
     * Цепочка блоков не может быть невалидна больше чем на одну связь
     */
    @VisibleForTesting
    void validateChains() throws ChainValidationException {
        if (blocks.isEmpty() || blocks.size() == 1) {
            return;
        }
        for (int i = blocks.size() - 1; i >= 1; i--) {
            Block prevBlock = blocks.get(i - 1);
            Block curBlock = blocks.get(i);
            if (!Objects.equals(prevBlock.getHash(), curBlock.getPrevHash())) {
                throw new ChainValidationException(prevBlock.getIndex(), curBlock.getIndex());
            }
        }
    }

    @NotNull
    protected Block getLastBlock() {
        return blocks.get(blocks.size() - 1);
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
                lock.lock();
                try {
                    Block prevBlock = getLastBlock();
                    Block generatedBlock = BlockGenerationUtils.generateBlock(prevBlock);

                    add(generatedBlock);

                    System.out.println("Broadcasting mined block: +" + generatedBlock);
                    rpcClient.sendBlockBroadcast(generatedBlock);

                    Thread.sleep(DELAY);
                } catch (ChainValidationException e) {
                    System.err.println("Chain validation failed: " + e.getMessage());
                    // TODO do something
                } catch (InterruptedException e) {
                    System.err.println("Thread was interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
