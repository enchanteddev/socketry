package com.socketry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.socketry.packetparser.Packet;
import com.socketry.socket.SocketDebug;

public class LinkFlowTest {
    @Test
    public void twoPackets() throws Exception {
        Packet.Init initPacket = new Packet.Init(TestUtilities.getRandomBytesForTesting(10));
        Packet.Call callPacket = new Packet.Call((byte) 1, (byte) 2, TestUtilities.getRandomBytesForTesting(10));
        
        SocketDebug socket = new SocketDebug();
        Link link = new Link(socket);
        
        ByteBuffer buffer = TestUtilities.wrapPacketForSending(initPacket);
        socket.writeOtherSide(buffer);

        buffer = TestUtilities.wrapPacketForSending(callPacket);
        socket.writeOtherSide(buffer);

        ArrayList<Packet> received = link.getPackets();

        assertEquals(2, received.size());

        TestUtilities.assertPacketsEqual(initPacket, received.get(0));
        TestUtilities.assertPacketsEqual(callPacket, received.get(1));
    }

    @Test
    public void oneLargePacket() throws Exception {
        Packet.Call callPacket = new Packet.Call((byte) 1, (byte) 2, TestUtilities.getRandomBytesForTesting(123456));
        
        SocketDebug socket = new SocketDebug();
        Link link = new Link(socket);
        
        ByteBuffer buffer = TestUtilities.wrapPacketForSending(callPacket);

        ExecutorService testExecutor = Executors.newFixedThreadPool(2);

        Future<Void> writeThread = testExecutor.submit(() -> {
            try {
                while (buffer.hasRemaining()) {
                    socket.writeOtherSide(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });

        Future<ArrayList<Packet>> readThread = testExecutor.submit(() -> {
            ArrayList<Packet> received = link.getPackets();
            int maxAttempts = 5000; // so that when it fails it does not crash everything
            while (received.isEmpty() && maxAttempts > 0) { 
                received = link.getPackets();
                maxAttempts--;
            }
            return received;
        });

        ArrayList<Packet> received = readThread.get();
        writeThread.get();

        assertEquals(1, received.size());

        TestUtilities.assertPacketsEqual(callPacket, received.get(0));
    }

    @Test
    public void twoPacketsBoundary() throws Exception {
        // leave 1 byte for the buffer
        Packet.Call callPacket = new Packet.Call((byte) 1, (byte) 2, TestUtilities.getRandomBytesForTesting(1023 - 7));
        // size of this packet does not matter
        Packet.Result resultPacket = new Packet.Result((byte) 1, (byte) 2, TestUtilities.getRandomBytesForTesting(1024));

        SocketDebug socket = new SocketDebug();
        Link link = new Link(socket);

        ByteBuffer buffer = TestUtilities.wrapPacketForSending(callPacket);
        socket.writeOtherSide(buffer);

        buffer = TestUtilities.wrapPacketForSending(resultPacket);
        socket.writeOtherSide(buffer);

        ArrayList<Packet> received = link.getPackets();

        assertEquals(2, received.size());

        TestUtilities.assertPacketsEqual(callPacket, received.get(0));
        TestUtilities.assertPacketsEqual(resultPacket, received.get(1));
    }
}
