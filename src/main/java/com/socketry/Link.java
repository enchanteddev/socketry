package com.socketry;

import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import com.socketry.packetparser.Packet;

public class Link {
    public Link(int port) {

    }

    public void register(Selector selector) {

    }

    public CompletableFuture<ArrayList<Packet>> getPackets() {
        return null;
    }

    public Packet getPacket() {
        return null;
    }

    public CompletableFuture<Void> sendPacket(Packet packet) {
        return null;
    }
}
