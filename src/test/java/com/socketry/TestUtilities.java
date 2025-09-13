package com.socketry;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.socketry.packetparser.Packet;

public class TestUtilities {
    static byte[] getRandomBytesForTesting(int length) {
        Random random = new Random();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    static short[] getRandomShortsForTesting(int length) {
        Random random = new Random();
        short[] shorts = new short[length];
        for (int i = 0; i < length; i++) {
            shorts[i] = (short) random.nextInt(Short.MAX_VALUE + 1);
        }
        return shorts;
    }

    static ByteBuffer wrapPacketForSending(Packet packet) {
        byte[] data = Packet.serialize(packet).array();
        ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        System.out.println("first 4 bytes: " + buffer.array()[0] + " " + buffer.array()[1] + " " + buffer.array()[2] + " " + buffer.array()[3]);
        return buffer;
    }

    static void assertPacketsEqual(Packet expected, Packet actual) {
        assertEquals(expected.getClass(), actual.getClass());
        switch (expected) {
            case Packet.Call callPacket -> {
                Packet.Call actualCallPacket = (Packet.Call) actual;
                assertEquals(callPacket.fnId(), actualCallPacket.fnId());
                assertEquals(callPacket.callId(), actualCallPacket.callId());
                assertArrayEquals(callPacket.arguments(), actualCallPacket.arguments());
            }
            case Packet.Result resultPacket -> {
                Packet.Result actualResultPacket = (Packet.Result) actual;
                assertEquals(resultPacket.fnId(), actualResultPacket.fnId());
                assertEquals(resultPacket.callId(), actualResultPacket.callId());
                assertArrayEquals(resultPacket.response(), actualResultPacket.response());
            }
            case Packet.Error errorPacket -> {
                Packet.Error actualErrorPacket = (Packet.Error) actual;
                assertEquals(errorPacket.fnId(), actualErrorPacket.fnId());
                assertEquals(errorPacket.callId(), actualErrorPacket.callId());
                assertArrayEquals(errorPacket.error(), actualErrorPacket.error());
            }
            case Packet.Init initPacket -> {
                Packet.Init actualInitPacket = (Packet.Init) actual;
                assertArrayEquals(initPacket.channels(), actualInitPacket.channels());
            }
            case Packet.Accept acceptPacket -> {
                Packet.Accept actualAcceptPacket = (Packet.Accept) actual;
                assertArrayEquals(acceptPacket.ports(), actualAcceptPacket.ports());
            }
            case Packet.Ping pingPacket -> {
            }
            case Packet.Pong pongPacket -> {
            }
        }
    }
}
