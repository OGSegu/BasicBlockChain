package org.main.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.main.entity.Block;
import org.main.exception.ChainValidationException;
import org.main.grpc.RpcClient;
import org.main.grpc.entity.GetBlockChainResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockChainServiceTest {

    private RpcClient rpcClient;
    private BlockChainService blockChainService;

    @BeforeEach
    void setUp() {
        rpcClient = mock(RpcClient.class);
        blockChainService = new BlockChainService(false, rpcClient);
    }

    @Test
    @DisplayName("Should return true and add the block when the block is not already mined")
    void onBlockReceivedWhenBlockIsNotMined() {
        Block block = new Block(1, "prevHash", "hash", 1L, "data");
        List<Block> receivedBlockchain = new ArrayList<>();
        receivedBlockchain.add(block);

        when(rpcClient.getBlockchain(anyLong()))
                .thenReturn(new GetBlockChainResponse(receivedBlockchain));

        boolean result = blockChainService.onBlockReceived(block);

        assertTrue(result);
        assertEquals(1, blockChainService.chainSize());
        assertEquals(block, blockChainService.getBlockChain(0).get(0));
    }

    @Test
    @DisplayName("Should return false and not add the block when the block is already mined")
    void onBlockReceivedWhenBlockIsAlreadyMined() {
        Block genesisBlock = new Block(0, "stub", "hash0", 1L, "data0");
        Block block1 = new Block(1, "hash0", "hash1", 1L, "data1");
        Block block2 = new Block(2, "hash1", "hash2", 2L, "data2");
        List<Block> blocks = new ArrayList<>();
        blocks.add(genesisBlock);
        blocks.add(block1);
        blocks.add(block2);

        GetBlockChainResponse response = new GetBlockChainResponse(blocks);
        when(rpcClient.getBlockchain(anyLong())).thenReturn(response);

        // Load the blocks into the blockchain service
        blockChainService.loadBlockchainFromCluster(0);

        boolean result = blockChainService.onBlockReceived(block2);

        assertFalse(result, "The block should not be added as it is already mined");
        assertEquals(3, blockChainService.chainSize(), "The chain size should remain the same");
    }

    @Test
    @DisplayName("Should not add any blocks if the received blockchain is empty")
    void loadBlockchainFromClusterWithEmptyBlockchain() {
        when(rpcClient.getBlockchain(anyLong())).thenReturn(GetBlockChainResponse.EMPTY);

        assertDoesNotThrow(() -> blockChainService.loadBlockchainFromCluster(0));

        assertEquals(0, blockChainService.chainSize());
    }

    @Test
    @DisplayName(
            "Should load the blockchain from the cluster and add the blocks to the local blockchain")
    void loadBlockchainFromClusterAndAddBlocks() { // Prepare the test data
        List<Block> receivedBlocks = new ArrayList<>();
        receivedBlocks.add(new Block(0, null, "hash0", 0L, "data0"));
        receivedBlocks.add(new Block(1, "hash0", "hash1", 1L, "data1"));
        receivedBlocks.add(new Block(2, "hash1", "hash2", 2L, "data2"));

        GetBlockChainResponse blockChainResponse = new GetBlockChainResponse(receivedBlocks);

        // Mock the rpcClient.getBlockchain method to return the prepared test data
        when(rpcClient.getBlockchain(anyLong())).thenReturn(blockChainResponse);

        // Call the loadBlockchainFromCluster method
        assertDoesNotThrow(() -> blockChainService.loadBlockchainFromCluster(0));

        // Verify that the local blockchain contains the received blocks
        assertEquals(3, blockChainService.chainSize());
        assertEquals(receivedBlocks, blockChainService.getBlockChain(0));
    }

    @Test
    @DisplayName(
            "Should validate the chains without throwing an exception when the chains are valid")
    void validateChainsWhenChainsAreValid() {
        Block genesisBlock = new Block(0, null, "Genesis Block");
        Block block1 = new Block(1, genesisBlock.getHash(), "Block 1");
        Block block2 = new Block(2, block1.getHash(), "Block 2");

        blockChainService.addFailSafe(block1);
        blockChainService.addFailSafe(block2);

        assertDoesNotThrow(blockChainService::validateChains);
    }

    @Test
    @DisplayName("Should throw a ChainValidationException when the chains are not valid")
    void validateChainsWhenChainsAreNotValidThenThrowException() {
        BlockChainService blockChainService = new BlockChainService(true, null);
        Block block1 = new Block(1, "prevHash1", "hash1", 1L, "data1");
        Block block2 = new Block(2, "prevHash2", "hash2", 2L, "data2");
        Block block3 = new Block(3, "prevHash3", "hash3", 3L, "data3");

        blockChainService.addFailSafe(block1);
        blockChainService.addFailSafe(block2);
        blockChainService.addFailSafe(block3);

        // Act and Assert
        assertThrows(ChainValidationException.class, blockChainService::validateChains);
    }

    @Test
    @DisplayName(
            "Should validate the chain with a single block (genesis block) without throwing an exception")
    void validateChainsWithSingleBlock() {
        Block genesisBlock = new Block(0, null, "hash", 0L, "Genesis Block");

        blockChainService.addFailSafe(genesisBlock);

        assertDoesNotThrow(blockChainService::validateChains);
    }

    @Test
    @DisplayName("Should throw a ChainValidationException when the chain has a block with an incorrect previous hash")
    void validateChainsWithIncorrectPreviousHash() {
        Block genesisBlock = new Block(0, null, "hash", 0L, "Genesis Block");
        Block block1 = new Block(1, genesisBlock.getHash(), "hash2", 0L, "Block 1");
        Block block2 = new Block(2, "incorrectPrevHash", "hash3", 0L, "Block 2");

        blockChainService.addFailSafe(genesisBlock);
        blockChainService.addFailSafe(block1);
        blockChainService.addFailSafe(block2);

        assertThrows(ChainValidationException.class, blockChainService::validateChains);
    }

    @Test
    @DisplayName("Should throw a ChainValidationException when the chain has a block with an incorrect index")
    void validateChainsWithIncorrectIndex() {
        Block genesisBlock = new Block(0, null, "Genesis Block");
        Block block1 = new Block(1, genesisBlock.getHash(), "Block 1");
        Block block2 = new Block(3, block1.getHash(), "Block 2");

        blockChainService.addFailSafe(genesisBlock);
        blockChainService.addFailSafe(block1);
        blockChainService.addFailSafe(block2);

        assertThrows(ChainValidationException.class, blockChainService::validateChains);
    }
}