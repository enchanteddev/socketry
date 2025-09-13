package com.socketry;

import java.nio.ByteBuffer;
import java.util.Random;

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
        return buffer;
    }
}
