package org.main;


import org.jetbrains.annotations.NotNull;
import org.main.grpc.RpcClient;
import org.main.grpc.RpcServer;
import org.main.state.BlockChain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Properties properties = loadProperties();
        String nodeName = getNodeName(args);

        RpcClient rpcClient = new RpcClient(nodeName, properties);

        BlockChain blockchain = new BlockChain(true, rpcClient);
        blockchain.start();

        int nodePort = Integer.parseInt(properties.getProperty(nodeName).split(":")[1]);
        RpcServer rpcServer = new RpcServer(blockchain, nodePort);
        rpcServer.start();
    }

    private static String getNodeName(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-n")) {
                if (i + 1 > args.length - 1) {
                    throw new IllegalArgumentException("No name is defined");
                }
                return args[i + 1];
            }
        }
        throw new IllegalArgumentException("No name is defined");
    }

    @NotNull
    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        InputStream resourceAsStream = Main.class.getClassLoader().getResourceAsStream("config.properties");
        properties.load(resourceAsStream);
        return properties;
    }
}