package org.main;


import org.main.state.BasicBlockchain;

public class Main {
    public static void main(String[] args) {
        BasicBlockchain basicBlockchain = new BasicBlockchain(true);
        basicBlockchain.start();
    }
}