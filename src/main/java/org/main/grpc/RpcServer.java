package org.main.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.main.state.BlockChain;

import java.io.IOException;
import java.util.Properties;

public class RpcServer {

    private final Server server;

    public RpcServer(BlockChain blockchain, Properties properties) {
        int port = Integer.parseInt(properties.getProperty(RpcConfiguration.PORT_PROPERTY));
        this.server = ServerBuilder.forPort(port)
                .addService(new RpcBlockService(blockchain))
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started...");
    }

}
