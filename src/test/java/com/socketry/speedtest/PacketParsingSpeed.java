package com.socketry.speedtest;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.socketry.TestUtilities;
import com.socketry.packetparser.Packet;
import com.socketry.socket.SocketDebug;

public class PacketParsingSpeed {

    @Test
    public void callPacketSpeed() throws Exception{
        SocketDebug socket = new SocketDebug();
        Packet.Call callPacket = new Packet.Call((byte) 1, (byte) 2, TestUtilities.getRandomBytesForTesting(10));
        ByteBuffer b = Packet.serializeFast(callPacket);
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            ByteBuffer buffer = Packet.serializeFast(callPacket);
            socket.writeOtherSide(buffer);
        }
        long end = System.nanoTime();
        System.out.println("Time taken for 1000 calls : " + (end - start));
    }

}
