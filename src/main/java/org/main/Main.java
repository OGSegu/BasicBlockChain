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

        RpcClient rpcClient = new RpcClient(properties);

        BlockChain blockchain = new BlockChain(true, rpcClient);
        blockchain.start();

        RpcServer rpcServer = new RpcServer(blockchain, properties);
        rpcServer.start();
    }

    @NotNull
    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        InputStream resourceAsStream = Main.class.getClassLoader().getResourceAsStream("config.properties");
        properties.load(resourceAsStream);
        return properties;
    }
}