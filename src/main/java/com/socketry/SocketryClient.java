package com.socketry;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.socketry.packetparser.Packet;

public class SocketryClient extends Socketry {
    public SocketryClient(byte[] socketsPerChannel, int server_port,
            HashMap<String, Function<byte[], byte[]>> procedures)
            throws IOException, InterruptedException, ExecutionException {
        
        Link link = new Link(server_port);

        Packet initPacket = new Packet.Init(socketsPerChannel);
        link.sendPacket(initPacket).get();
        
        Packet acceptPacket = link.getPacket();
        if (!(acceptPacket instanceof Packet.Accept)) {
            throw new IllegalStateException("Expected accept packet");
        }

        this.setProcedures(procedures);
    }
}
