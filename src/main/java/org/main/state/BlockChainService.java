package org.main.state;


import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.main.BlockGenerationUtils;
import org.main.entity.Block;
import org.main.exception.ChainValidationException;
import org.main.grpc.RpcClient;
import org.main.grpc.entity.GetBlockChainResponse;
import org.main.grpc.entity.MinedBlockRequest;
import org.main.grpc.entity.MinedBlockResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class BlockChainService {

    /**
     * Обязательно использовать под {@link BlockChainService#lock}
     */
    protected final List<Block> blocks;
    private final ExecutorService mainWorker;
    private final RpcClient rpcClient;

    private final ReentrantLock lock = new ReentrantLock(true);
    private final AtomicInteger blockRequestsIncoming = new AtomicInteger(0);
    private final boolean generateGenesis;

    public BlockChainService(boolean generateGenesis, RpcClient rpcClient) {
        this.blocks = new ArrayList<>();
        this.mainWorker = Executors.newSingleThreadExecutor();
        this.rpcClient = rpcClient;
        this.generateGenesis = generateGenesis;

        if (generateGenesis) {
            Block genesis = BlockGenerationUtils.generateGenesis();
            blocks.add(genesis);
        }
    }
    public void start() {
        if (!generateGenesis) {
            loadBlockchainFromCluster();
        }
        mainWorker.submit(new BlockChainMainWorker());
        System.out.println("Blockchain started...");
    }

    private void loadBlockchainFromCluster() {
        System.out.println("Trying to get blockchain from cluster...");

        lock.lock();
        try {
            GetBlockChainResponse blockChainResponse = rpcClient.getBlockchain();
            List<Block> receivedBlockchain = blockChainResponse.blockChain();
            if (receivedBlockchain.isEmpty()) {
                return;
            }

            for (Block block : blockChainResponse.blockChain()) {
                try {
                    add(block);
                } catch (ChainValidationException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.printf("Successfully added [%d] blocks from cluster%n", receivedBlockchain.size());
        } finally {
            lock.unlock();
        }
    }

    public long chainSize() {
        return blocks.size();
    }

    public List<Block> getBlockChain() {
        return ImmutableList.copyOf(blocks);
    }

    public void onBlockRequestReceived(MinedBlockRequest request) throws ChainValidationException {
        onBlockReceived(request.block());
    }

    public void onBlockReceived(Block block) throws ChainValidationException {
        try {
            long blockIndex = block.getIndex();
            if (blocks.size() - 1 >= blockIndex) {
                System.out.printf("Block with index [%d] is already mined. Skipping...%n", blockIndex);
                return;
            }
            blockRequestsIncoming.incrementAndGet();
            lock.lock();
            try {
                add(block);
                System.out.printf("Added received block with index: [%d]%n", block.getIndex());
            } finally {
                lock.unlock();
            }
        } finally {
            blockRequestsIncoming.decrementAndGet();
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

        private static final long DELAY = 4000L;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {

                lock.lock();
                try {
                    Block prevBlock = getLastBlock();
                    Block generatedBlock = BlockGenerationUtils.generateBlock(prevBlock, () -> blockRequestsIncoming.get() > 0);
                    if (generatedBlock == null) {
                        System.out.printf("Block mining was finished. Block requests: [%d]%n",
                                blockRequestsIncoming.get());
                        continue;
                    }

                    System.out.printf("Generated block with index: [%d]%n", generatedBlock.getIndex());
                    List<MinedBlockResponse> responses = rpcClient.sendBlockBroadcast(generatedBlock);

                    add(generatedBlock);
                } catch (ChainValidationException e) {
                    System.err.println("Chain validation failed: " + e.getMessage());
                    // TODO do something
                } finally {
                    lock.unlock();
                }
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
