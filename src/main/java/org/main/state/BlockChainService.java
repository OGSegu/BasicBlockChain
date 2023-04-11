package org.main.state;


import com.google.common.base.Joiner;
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
import org.main.grpc.entity.MinedBlockResponseCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
            loadBlockchainFromCluster(0); // whole blockchain
        }
        mainWorker.submit(new BlockChainMainWorker());
        System.out.println("Blockchain started...");
    }

    private void loadBlockchainFromCluster(long fromIndex) {
        System.out.printf("Trying to get blockchain from index: [%d] from cluster...%n", fromIndex);

        lock.lock();
        try {
            GetBlockChainResponse blockChainResponse = rpcClient.getBlockchain(fromIndex);
            List<Block> receivedBlockchain = blockChainResponse.blockChain();
            if (receivedBlockchain.isEmpty()) {
                return;
            }

            for (Block block : blockChainResponse.blockChain()) {
                add(block);
            }
            System.out.printf("Successfully added [%d] blocks from cluster%n", receivedBlockchain.size());
        } finally {
            lock.unlock();
        }
    }

    public long chainSize() {
        return blocks.size();
    }

    public List<Block> getBlockChain(long fromIndex) {
        return ImmutableList.copyOf(blocks.subList((int) fromIndex, blocks.size()));
    }

    public boolean onBlockRequestReceived(MinedBlockRequest request) {
        return onBlockReceived(request.block());
    }

    public boolean onBlockReceived(Block block) {
        try {
            long blockIndex = block.getIndex();
            if (blocks.size() - 1 >= blockIndex) {
                System.out.printf("Block with index [%d] is already mined. Rejected...%n", blockIndex);
                return false;
            }
            blockRequestsIncoming.incrementAndGet();
            lock.lock();
            try {
                add(block);
                System.out.printf("Added received block with index: [%d]%n", block.getIndex());
                return true;
            } finally {
                lock.unlock();
            }
        } finally {
            blockRequestsIncoming.decrementAndGet();
        }
    }

    @VisibleForTesting
    boolean add(Block newBlock) {
        blocks.add(newBlock);
        try {
            validateChains();
        } catch (ChainValidationException e) {
            blocks.remove(blocks.size() - 1); // добавленный блок "сломал" цепочку
            return false;
        }

        if (newBlock.getIndex() % 10 == 0) {
            System.out.printf("Blockchain state: [%s]%n", Joiner.on(";").join(blocks));
        }

        return true;
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

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                lock.lock();
                try {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(4000, 15000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    Block prevBlock = getLastBlock();
                    Block generatedBlock = BlockGenerationUtils.generateBlock(prevBlock, () -> blockRequestsIncoming.get() > 0);
                    if (generatedBlock == null) {
                        System.out.printf("Block mining was finished. Block requests: [%d]%n",
                                blockRequestsIncoming.get());
                        continue;
                    }

                    System.out.printf("Generated block with index: [%d]%n", generatedBlock.getIndex());

                    List<MinedBlockResponse> responses = rpcClient.sendBlockBroadcast(generatedBlock);

                    System.out.printf("Block broadcast returned: [%s]%n", Joiner.on(";").join(responses));

                    Map<MinedBlockResponseCode, List<MinedBlockResponse>> responsesByCode = responses.stream()
                            .filter(response -> response.getResponseCode() != MinedBlockResponseCode.FAILED) // failed doesn't contain any useful info
                            .collect(Collectors.groupingBy(MinedBlockResponse::getResponseCode));

                    List<MinedBlockResponse> rejectedResponses = responsesByCode.get(MinedBlockResponseCode.REJECTED);
                    int rejected = rejectedResponses == null ? -1 : rejectedResponses.size();
                    List<MinedBlockResponse> acceptedResponses = responsesByCode.get(MinedBlockResponseCode.ACCEPTED);
                    int accepted =  acceptedResponses == null ? -1 : acceptedResponses.size();

                    boolean isRejected = rejected == responsesByCode.size();

                    if (isRejected) {
                        loadBlockchainFromCluster(generatedBlock.getIndex());
                        continue;
                    }

                    boolean isAccepted = accepted >= Math.ceil((double) responsesByCode.size() / 2);

                    if (isAccepted || responsesByCode.isEmpty()) {
                        add(generatedBlock);
                    }

                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
