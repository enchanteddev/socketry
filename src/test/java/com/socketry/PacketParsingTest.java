package com.socketry;

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

        assertEquals((parsedPacket).channels().length, randomBytes.length);
        for (int i = 0; i < randomBytes.length; i++) {
            assertEquals((parsedPacket).channels()[i], randomBytes[i]);
        }
    }

    @Test
    public void sanityCall() {
        byte[] randomBytes = getRandomBytesForTesting(10);
        
        Packet.Call call = new Packet.Call((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(call).array();
        Packet parsed = Packet.parse(data);
        
        assertTrue(parsed instanceof Packet.Call);
        Packet.Call parsedPacket = (Packet.Call) parsed;

        assertEquals((parsedPacket).fnId(), 1);
        assertEquals((parsedPacket).callId(), 2);
        assertEquals((parsedPacket).arguments().length, randomBytes.length);
        for (int i = 0; i < randomBytes.length; i++) {
            assertEquals((parsedPacket).arguments()[i], randomBytes[i]);
        }
    }

    @Test
    public void sanityResult() {
        byte[] randomBytes = getRandomBytesForTesting(10);
        
        Packet.Result result = new Packet.Result((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(result).array();
        Packet parsed = Packet.parse(data);
        
        assertTrue(parsed instanceof Packet.Result);
        Packet.Result parsedPacket = (Packet.Result) parsed;

        assertEquals((parsedPacket).fnId(), 1);
        assertEquals((parsedPacket).callId(), 2);
        assertEquals((parsedPacket).response().length, randomBytes.length);
        for (int i = 0; i < randomBytes.length; i++) {
            assertEquals((parsedPacket).response()[i], randomBytes[i]);
        }
    }

    @Test
    public void sanityError() {
        byte[] randomBytes = getRandomBytesForTesting(10);

        Packet.Error error = new Packet.Error((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(error).array();
        Packet parsed = Packet.parse(data);
        
        assertTrue(parsed instanceof Packet.Error);
        Packet.Error parsedPacket = (Packet.Error) parsed;

        assertEquals((parsedPacket).fnId(), 1);
        assertEquals((parsedPacket).callId(), 2);
        assertEquals((parsedPacket).error().length, randomBytes.length);
        for (int i = 0; i < randomBytes.length; i++) {
            assertEquals((parsedPacket).error()[i], randomBytes[i]);
        }
    }

    @Test
    public void sanityAccept() {
        short[] shorts = getRandomShortsForTesting(10);
        Packet.Accept accept = new Packet.Accept(shorts);
        
        byte[] data = Packet.serialize(accept).array();
        Packet parsed = Packet.parse(data);

        assertTrue(parsed instanceof Packet.Accept);
        Packet.Accept parsedPacket = (Packet.Accept) parsed;
        
        assertEquals((parsedPacket).ports().length, shorts.length);
        for (int i = 0; i < shorts.length; i++) {
            assertEquals((parsedPacket).ports()[i], shorts[i]);
        }
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
