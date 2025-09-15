package com.socketry.StressTests;

import org.junit.jupiter.api.Test;

public class SocketryStressRunner {
    @Test
    public void stressTestServerCLient() {

        Thread serverThread = new Thread(() -> {
            try {
                SocketryStressServer.start(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        Thread clientThread = new Thread(() -> {
            try {
                SocketryStressClient.start(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        clientThread.start();

        try {
            serverThread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
