package com.socketry;

import org.junit.jupiter.api.Test;

import com.socketry.packetparser.Packet;

/**
 * Parsing test for all packet types.
 */
public class PacketParsingTest {
    @Test
    public void sanityInit() {
        byte[] randomBytes = TestUtilities.getRandomBytesForTesting(10);

        Packet.Init init = new Packet.Init(randomBytes);
        byte[] data = Packet.serialize(init).array();
        Packet parsed = Packet.parse(data);

        TestUtilities.assertPacketsEqual(init, parsed);
    }

    @Test
    public void sanityCall() {
        byte[] randomBytes = TestUtilities.getRandomBytesForTesting(10);

        Packet.Call call = new Packet.Call((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(call).array();
        Packet parsed = Packet.parse(data);

        TestUtilities.assertPacketsEqual(call, parsed);

    }

    @Test
    public void sanityResult() {
        byte[] randomBytes = TestUtilities.getRandomBytesForTesting(10);

        Packet.Result result = new Packet.Result((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(result).array();
        Packet parsed = Packet.parse(data);

        TestUtilities.assertPacketsEqual(result, parsed);
    }

    @Test
    public void sanityError() {
        byte[] randomBytes = TestUtilities.getRandomBytesForTesting(10);

        Packet.Error error = new Packet.Error((byte) 1, (byte) 2, randomBytes);
        byte[] data = Packet.serialize(error).array();
        Packet parsed = Packet.parse(data);

        TestUtilities.assertPacketsEqual(error, parsed);
    }

    @Test
    public void sanityAccept() {
        short[] randomShorts = TestUtilities.getRandomShortsForTesting(10);
        Packet.Accept accept = new Packet.Accept(randomShorts);

        byte[] data = Packet.serialize(accept).array();
        Packet parsed = Packet.parse(data);

        TestUtilities.assertPacketsEqual(accept, parsed);
    }

    @Test
    public void sanityPing() {
        Packet ping = Packet.Ping.INSTANCE;
        byte[] data = Packet.serialize(ping).array();
        Packet parsed = Packet.parse(data);
        TestUtilities.assertPacketsEqual(ping, parsed);
    }

    @Test
    public void sanityPong() {
        Packet pong = Packet.Pong.INSTANCE;
        byte[] data = Packet.serialize(pong).array();
        Packet parsed = Packet.parse(data);
        TestUtilities.assertPacketsEqual(pong, parsed);
    }
}
