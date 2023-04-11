package org.main;


import org.jetbrains.annotations.NotNull;
import org.main.grpc.RpcClient;
import org.main.grpc.RpcServer;
import org.main.state.BlockChainService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Main {

    private static final String NODE_NAME_ARG = "-n";
    private static final String GENESIS_GENERATOR_ARG = "-g";

    public static void main(String[] args) throws IOException {
        Properties properties = loadProperties();
        Map<String, String> parsedArgs = getNodeName(args);
        String nodeName = parsedArgs.get(NODE_NAME_ARG);
        boolean genesisGeneratorNode = parsedArgs.containsKey(GENESIS_GENERATOR_ARG);


        RpcClient rpcClient = new RpcClient(nodeName, properties);

        BlockChainService blockchain = new BlockChainService(genesisGeneratorNode, rpcClient);
        blockchain.start();

        int nodePort = Integer.parseInt(properties.getProperty(nodeName).split(":")[1]);
        RpcServer rpcServer = new RpcServer(blockchain, nodePort);
        rpcServer.start();
    }

    private static Map<String, String> getNodeName(String[] args) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(NODE_NAME_ARG)) {
                if (i + 1 > args.length - 1) {
                    throw new IllegalArgumentException("No name is defined");
                }
                result.put(NODE_NAME_ARG, args[i + 1]);
                continue;
            }
            if (args[i].equalsIgnoreCase(GENESIS_GENERATOR_ARG)) {
                result.put(GENESIS_GENERATOR_ARG, "stub");
            }
        }
        return result;
    }

    @NotNull
    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        InputStream resourceAsStream = Main.class.getClassLoader().getResourceAsStream("config.properties");
        properties.load(resourceAsStream);
        return properties;
    }
}