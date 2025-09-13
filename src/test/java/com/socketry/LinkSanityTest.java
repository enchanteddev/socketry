package com.socketry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.socketry.packetparser.Packet;
import com.socketry.socket.SocketDebug;

public class LinkSanityTest {
    @Test
    public void sanityInit() throws Exception {
        byte[] socketsPerTunnel = TestUtilities.getRandomBytesForTesting(10);

        SocketDebug socket = new SocketDebug();
        Link link = new Link(socket);

        Packet.Init packet = new Packet.Init(socketsPerTunnel);

        socket.writeOtherSide(TestUtilities.wrapPacketForSending(packet));

        ArrayList<Packet> received = link.getPackets();

        assertEquals(1, received.size());

        assertTrue(received.get(0) instanceof Packet.Init);    
        Packet.Init receivedPacket = (Packet.Init) received.get(0);
    
        assertArrayEquals(packet.channels(), receivedPacket.channels());
    }

    @Test
    public void sanityCall() throws Exception {
        byte[] arguments = TestUtilities.getRandomBytesForTesting(10);
        byte fnId = 1;
        byte callId = 2;

        Packet.Call packet = new Packet.Call(fnId, callId, arguments);

        SocketDebug socket = new SocketDebug();
        Link link = new Link(socket);

        socket.writeOtherSide(TestUtilities.wrapPacketForSending(packet));

        ArrayList<Packet> received = link.getPackets();

        assertEquals(1, received.size());

        assertTrue(received.get(0) instanceof Packet.Call);
        Packet.Call receivedPacket = (Packet.Call) received.get(0);

        assertEquals(fnId, receivedPacket.fnId());
        assertEquals(callId, receivedPacket.callId());
        assertArrayEquals(arguments, receivedPacket.arguments());
    }

    @Test
    public void sanityAccept() throws Exception {
        short[] ports = TestUtilities.getRandomShortsForTesting(10);
        Packet.Accept packet = new Packet.Accept(ports);

        SocketDebug socket = new SocketDebug();
        Link link = new Link(socket);

        socket.writeOtherSide(TestUtilities.wrapPacketForSending(packet));

        ArrayList<Packet> received = link.getPackets();

        assertEquals(1, received.size());

        assertTrue(received.get(0) instanceof Packet.Accept);
        Packet.Accept receivedPacket = (Packet.Accept) received.get(0);

        assertArrayEquals(ports, receivedPacket.ports());
    }

    @Test
    public void sanityPing() throws Exception {
        Packet.Ping packet = Packet.Ping.INSTANCE;

        SocketDebug socket = new SocketDebug();
        Link link = new Link(socket);

        socket.writeOtherSide(TestUtilities.wrapPacketForSending(packet));

        ArrayList<Packet> received = link.getPackets();

        assertEquals(1, received.size());

        assertTrue(received.get(0) instanceof Packet.Ping);
    }
}
