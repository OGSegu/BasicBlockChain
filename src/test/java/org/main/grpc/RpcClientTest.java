package org.main.grpc;

import com.google.protobuf.Empty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.main.entity.Block;
import org.main.grpc.entity.GetBlockChainResponse;
import org.main.grpc.entity.HeartbeatResponse;
import org.main.grpc.entity.MinedBlockResponse;
import org.main.grpc.entity.MinedBlockResponseCode;
import org.main.java.grpc.BlockServiceGrpc;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RpcClientTest {

    @Test
    @DisplayName(
            "Should return a map with stubs and heartbeat responses when sending heartbeat broadcast")
    void sendHeartbeatBroadcastReturnsMapWithStubsAndResponses() { // Prepare test data
        Properties properties = new Properties();
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "2");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "0", "localhost:50051");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "1", "localhost:50052");

        RpcClient rpcClient = new RpcClient("node_0", properties);

        // Execute the method under test
        Map<BlockServiceGrpc.BlockServiceBlockingStub, HeartbeatResponse> heartbeatResponses =
                rpcClient.sendHeartbeatBroadcast();

        // Assert the results
        assertNotNull(heartbeatResponses, "Heartbeat responses map should not be null");
        assertEquals(1, heartbeatResponses.size(), "Heartbeat responses map should have 1 entry");
    }

    @Test
    @DisplayName(
            "Should handle exceptions when sending heartbeat broadcast and return a map with stubs and failed heartbeat responses")
    void
    sendHeartbeatBroadcastHandlesExceptionsAndReturnsMapWithStubsAndFailedResponses() { // Prepare test data
        Properties properties = new Properties();
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "2");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "0", "localhost:5000");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "1", "localhost:5001");

        RpcClient rpcClient = new RpcClient("node_0", properties);

        // Mock the stubs to throw exceptions when sendHeartbeat is called
        rpcClient.stubs =
                rpcClient.stubs.stream()
                        .map(
                                stub -> {
                                    BlockServiceGrpc.BlockServiceBlockingStub mockStub =
                                            Mockito.mock(
                                                    BlockServiceGrpc.BlockServiceBlockingStub
                                                            .class);
                                    Mockito.when(mockStub.sendHeartbeat(Empty.newBuilder().build()))
                                            .thenThrow(new RuntimeException("Test exception"));
                                    Mockito.when(mockStub.getChannel())
                                            .thenReturn(stub.getChannel());
                                    return mockStub;
                                })
                        .collect(Collectors.toList());

        // Call the method under test
        Map<BlockServiceGrpc.BlockServiceBlockingStub, HeartbeatResponse> result =
                rpcClient.sendHeartbeatBroadcast();

        // Assert the results
        assertNotNull(result, "Result should not be null");
        assertEquals(
                rpcClient.stubs.size(), result.size(), "Result size should match the stubs size");

        for (Map.Entry<BlockServiceGrpc.BlockServiceBlockingStub, HeartbeatResponse> entry :
                result.entrySet()) {
            assertTrue(rpcClient.stubs.contains(entry.getKey()), "Result should contain the stub");
            assertEquals(
                    -1L,
                    entry.getValue().chainLength(),
                    "Failed heartbeat response should have chainLength -1");
        }
    }

    @Test
    @DisplayName("Should return an empty response when no host with a valid chain length is found")
    void getBlockchainWhenNoHostWithValidChainLength() {
        Properties properties = new Properties();
        properties.setProperty("port", "8080");
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "0");

        RpcClient rpcClient = new RpcClient("node_0", properties);
        GetBlockChainResponse response = rpcClient.getBlockchain(0);

        assertEquals(
                GetBlockChainResponse.EMPTY,
                response,
                "Expected an empty response when no host with a valid chain length is found");
    }

    @Test
    @DisplayName("Should handle exceptions when getting the blockchain from a host")
    void getBlockchainWhenExceptionOccurs() {
        Properties properties = new Properties();
        properties.setProperty("port", "8080");
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "1");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "0", "localhost:8080");

        RpcClient rpcClient =
                new RpcClient("node_0", properties) {
                    @Override
                    protected void init(Properties properties) {
                        // Do not initialize stubs to simulate an exception when getting the
                        // blockchain
                    }
                };

        GetBlockChainResponse response = rpcClient.getBlockchain(0);
        assertEquals(
                GetBlockChainResponse.EMPTY, response, "Expected an empty GetBlockChainResponse");
    }

    @Test
    @DisplayName("Should return an empty list of MinedBlockResponse when there are no nodes")
    void sendBlockBroadcastWithNoNodes() {
        Properties properties = new Properties();
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "0");

        RpcClient rpcClient = new RpcClient("node_0", properties);
        Block block = new Block(0, "prev_hash", "hash", 0L, "data");

        List<MinedBlockResponse> result = rpcClient.sendBlockBroadcast(block);

        assertEquals(0, result.size(), "Expected an empty list of MinedBlockResponse");
    }

    @Test
    @DisplayName(
            "Should return a list of MinedBlockResponse with FAILED status when an exception occurs")
    void sendBlockBroadcastWithException() {
        Properties properties = new Properties();
        properties.setProperty("nodes_amount", "2");
        properties.setProperty("node_0", "localhost:5000");
        properties.setProperty("node_1", "localhost:5001");

        RpcClient rpcClient = new RpcClient("node_0", properties);
        Block block = new Block(0, "prev_hash", "hash", 0L, "data");

        List<MinedBlockResponse> responses = rpcClient.sendBlockBroadcast(block);

        assertEquals(1, responses.size(), "Expected 1 MinedBlockResponse");
        assertEquals(
                MinedBlockResponseCode.FAILED,
                responses.get(0).getResponseCode(),
                "Expected FAILED status");
    }

    @Test
    @DisplayName("Should send block broadcast to all nodes and return a list of MinedBlockResponse")
    void sendBlockBroadcastToAllNodes() { // Prepare test data
        Properties properties = new Properties();
        properties.setProperty("nodes_amount", "3");
        properties.setProperty("node_0", "localhost:5000");
        properties.setProperty("node_1", "localhost:5001");
        properties.setProperty("node_2", "localhost:5002");

        // Create RpcClient instance
        RpcClient rpcClient = new RpcClient("node_0", properties);

        // Create a sample block
        Block block = new Block(0, "prev_hash", "hash", 0L, "data");

        // Call sendBlockBroadcast method
        List<MinedBlockResponse> minedBlockResponses = rpcClient.sendBlockBroadcast(block);

        // Assert the results
        assertEquals(2, minedBlockResponses.size(), "Should return 2 MinedBlockResponse instances");
        for (MinedBlockResponse response : minedBlockResponses) {
            assertTrue(
                    response.getHost().startsWith("localhost"),
                    "Host should start with 'localhost'");
            assertEquals(
                    MinedBlockResponseCode.FAILED,
                    response.getResponseCode(),
                    "Response code should be FAILED");
            assertNull(response.getBlock(), "Block should be null");
        }
    }

    @Test
    @DisplayName("Should handle missing or invalid nodes amount property")
    void initWithMissingOrInvalidNodesAmountProperty() {
        Properties properties = new Properties();
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "0");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "0", "localhost:5000");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "1", "localhost:5001");

        RpcClient rpcClient = new RpcClient("node_0", properties);

        assertEquals(
                0,
                rpcClient.stubs.size(),
                "No stubs should be created when nodes amount property is missing or invalid");
    }

    @Test
    @DisplayName("Should handle invalid target address format in properties")
    void initWithInvalidTargetAddressFormat() {
        Properties properties = new Properties();
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "2");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "0", "localhost");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "1", "localhost:8080");

        RpcClient rpcClient = new RpcClient("node_0", properties);

        assertEquals(1, rpcClient.stubs.size(), "Only one valid target address should be added");
        assertEquals(
                "localhost:8080",
                rpcClient.stubs.get(0).getChannel().authority(),
                "The valid target address should be added");
    }

    @Test
    @DisplayName("Should skip the current node while initializing stubs")
    void initShouldSkipCurrentNode() {
        Properties properties = new Properties();
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "3");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "0", "localhost:5000");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "1", "localhost:5001");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "2", "localhost:5002");

        RpcClient rpcClient = new RpcClient("node_1", properties);

        assertEquals(2, rpcClient.stubs.size(), "Should have initialized stubs for 2 nodes");
        assertEquals(
                "localhost:5000",
                rpcClient.stubs.get(0).getChannel().authority(),
                "Should have initialized stub for node_0");
        assertEquals(
                "localhost:5002",
                rpcClient.stubs.get(1).getChannel().authority(),
                "Should have initialized stub for node_2");
    }

    @Test
    @DisplayName("Should initialize stubs with correct target addresses from properties")
    void initWithCorrectTargetAddressesFromProperties() {
        Properties properties = new Properties();
        properties.setProperty(RpcConfiguration.NODES_AMOUNT_PROPERTY, "3");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "0", "localhost:5000");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "1", "localhost:5001");
        properties.setProperty(RpcConfiguration.NODE_PREFIX_PROPERTY + "2", "localhost:5002");

        RpcClient rpcClient = new RpcClient("node_0", properties);

        assertEquals(2, rpcClient.stubs.size(), "Expected 2 stubs to be initialized");

        String firstStubAddress = rpcClient.stubs.get(0).getChannel().authority();
        String secondStubAddress = rpcClient.stubs.get(1).getChannel().authority();

        assertEquals(
                "localhost:5001",
                firstStubAddress,
                "Expected first stub to have target address 'localhost:5001'");
        assertEquals(
                "localhost:5002",
                secondStubAddress,
                "Expected second stub to have target address 'localhost:5002'");
    }
}