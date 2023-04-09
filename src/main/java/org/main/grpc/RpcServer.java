package org.main.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.main.state.BlockChain;

import java.io.IOException;

public class RpcServer {

    private final Server server;

    public RpcServer(BlockChain blockchain, int port) {
        System.out.printf("Created server on port %d%n", port);
        this.server = ServerBuilder.forPort(port)
                .addService(new RpcBlockService(blockchain))
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started...");
    }

}
