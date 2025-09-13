package com.socketry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.socketry.packetparser.Packet;

/**
 * Parsing test for all packet types.
 */
public class PacketParsingTest {
    byte[] getRandomBytesForTesting(int length) {
        Random random = new Random();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    short[] getRandomShortsForTesting(int length) {
        Random random = new Random();
        short[] shorts = new short[length];
        for (int i = 0; i < length; i++) {
            shorts[i] = (short) random.nextInt(Short.MAX_VALUE + 1);
        }
        return shorts;
    }

    @Test
    public void sanityInit() {
        byte[] randomBytes = getRandomBytesForTesting(10);
        
        Packet.Init init = new Packet.Init(randomBytes);
        byte[] data = Packet.serialize(init).array();
        Packet parsed = Packet.parse(data);
        
        assertTrue(parsed instanceof Packet.Init);
        Packet.Init parsedPacket = (Packet.Init) parsed;

        assertArrayEquals(randomBytes, (parsedPacket).channels());
    }

    @Test
    public void sanityCall() {
        byte[] randomBytes = getRandomBytesForTesting(10);
        
        Packet.Call call = new Packet.Call((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(call).array();
        Packet parsed = Packet.parse(data);
        
        assertTrue(parsed instanceof Packet.Call);
        Packet.Call parsedPacket = (Packet.Call) parsed;

        assertEquals(1, (parsedPacket).fnId());
        assertEquals(2, (parsedPacket).callId());
        assertArrayEquals(randomBytes, parsedPacket.arguments());
    }

    @Test
    public void sanityResult() {
        byte[] randomBytes = getRandomBytesForTesting(10);
        
        Packet.Result result = new Packet.Result((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(result).array();
        Packet parsed = Packet.parse(data);
        
        assertTrue(parsed instanceof Packet.Result);
        Packet.Result parsedPacket = (Packet.Result) parsed;

        assertEquals(1, (parsedPacket).fnId());
        assertEquals(2, (parsedPacket).callId());
        assertArrayEquals(randomBytes, (parsedPacket).response());
    }

    @Test
    public void sanityError() {
        byte[] randomBytes = getRandomBytesForTesting(10);

        Packet.Error error = new Packet.Error((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(error).array();
        Packet parsed = Packet.parse(data);
        
        assertTrue(parsed instanceof Packet.Error);
        Packet.Error parsedPacket = (Packet.Error) parsed;

        assertEquals(1, (parsedPacket).fnId());
        assertEquals(2, (parsedPacket).callId());
        assertArrayEquals(randomBytes, (parsedPacket).error());
    }

    @Test
    public void sanityAccept() {
        short[] randomShorts = getRandomShortsForTesting(10);
        Packet.Accept accept = new Packet.Accept(randomShorts);
        
        byte[] data = Packet.serialize(accept).array();
        Packet parsed = Packet.parse(data);

        assertTrue(parsed instanceof Packet.Accept);
        Packet.Accept parsedPacket = (Packet.Accept) parsed;
        
        assertArrayEquals(randomShorts, (parsedPacket).ports());
    }

    @Test
    public void sanityPing() {
        Packet ping = Packet.Ping.INSTANCE;
        byte[] data = Packet.serialize(ping).array();
        Packet parsed = Packet.parse(data);
        assertTrue(parsed instanceof Packet.Ping);
    }

    @Test
    public void sanityPong() {
        Packet pong = Packet.Pong.INSTANCE;
        byte[] data = Packet.serialize(pong).array();
        Packet parsed = Packet.parse(data);
        assertTrue(parsed instanceof Packet.Pong);
    }
}
