package com.socketry;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.socketry.packetparser.Packet;

record CallIdentifier(byte callId, byte fnId) {
    @Override
    public final int hashCode() {
        return callId << 8 | fnId;
    }
}

public class Tunnel {
    Map<CallIdentifier, CompletableFuture<byte[]>> packets;
    Queue<Packet> packetQueue; // worst case scenario is number of packets in transit > 255

    ArrayList<Link> Links;
    Selector selector;

    private void initialize() throws IOException {
        this.selector = Selector.open();
        this.packets = Collections.synchronizedMap(new HashMap<>());
        this.packetQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }

    /**
     * Creates a tunnel with given number of links.
     * Also creates a selector and registers all the created links to the selector
     * @param LinkPorts :
     * @throws IOException
     */
    public Tunnel(Short[] LinkPorts) throws IOException {
        initialize();
        this.Links = new ArrayList<>();
        for (Short linkPort : LinkPorts) {
            Link link = new Link(linkPort);
            link.register(selector);
            Links.add(link);
        }
    }
    /**
     * Creates a tunnel with given number of links.
     * Also creates a selector and registers all the created links to the selector
     * @param sockets :
     * @throws IOException
     */
    public Tunnel(SocketChannel[] sockets) throws IOException {
        initialize();
        this.Links = new ArrayList<>();
        for (SocketChannel socketChannel : sockets) {
            Link link = new Link(socketChannel);
            link.register(selector);
            Links.add(link);
        }
    }

    private Packet feedPacket(Packet packet) {
        switch (packet) {
            case Packet.Result resPacket -> {
                byte[] result = resPacket.response();
                CallIdentifier callIdentifier = new CallIdentifier(resPacket.callId(), resPacket.fnId());
                CompletableFuture<byte[]> resFuture = packets.get(callIdentifier);
                if (resFuture != null) {
                    resFuture.complete(result);
                }
                return null;
            }
            case Packet.Error errorPacket -> {
                byte[] error = errorPacket.error();
                CallIdentifier callIdentifier = new CallIdentifier(errorPacket.callId(), errorPacket.fnId());
                CompletableFuture<byte[]> resFuture = packets.get(callIdentifier);
                if (resFuture != null) {
                    resFuture.completeExceptionally(new Exception(new String(error)));
                }
                return null;
            }
            default -> {
                // leave it for socketry to handle
                return packet;
            }
        }
    }

    private Link selectLink() {
        int linkId = Math.max(0, (int) (Math.random() * Links.size()) - 1);
        return Links.get(linkId);
    }

    byte assignCallId(byte fnId) {
        for (byte i = 0; i < 255; i++) {
            if (!packets.containsKey(new CallIdentifier(i, fnId))) {
                return i;
            }
        }
        throw new IllegalStateException("No free callId available");
    }

    /**
     * send the packet via any of the link of the Tunnel
     * @param packet
     */
    public void sendPacket(Packet packet) {
        Link link = selectLink();
        link.sendPacket(packet);
    }

    public ArrayList<Packet> listen() {
        ArrayList<Packet> packets = new ArrayList<>();
        try {
            // Block until at least one channel is ready with some data to read
            int readyChannels = selector.select(100); // 100 milli-second timeout

            if (readyChannels == 0) {
                return new ArrayList<>();
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();

            for (SelectionKey key : selectedKeys) {
                if (key.isReadable()) {
                    Link link = (Link) key.attachment();
                    packets.addAll(link.getPackets());
                }
            }
        } catch (IOException e) {
            System.err.println("Error in selector loop: " + e.getMessage());
            e.printStackTrace();
        }

        ArrayList<Packet> packetsToReturn = new ArrayList<>();
        // feed each packet received
        for (Packet packet : packets) {
            Packet feededPacket = feedPacket(packet);
            if (feededPacket != null) {
                packetsToReturn.add(feededPacket);
            }
        }

        return packetsToReturn;
    }

    public CompletableFuture<byte[]> callFn(byte fnId, byte[] arguments) {

        byte callId = assignCallId(fnId);
        CallIdentifier callIdentifier = new CallIdentifier(callId, fnId);

        CompletableFuture<byte[]> resFuture = new CompletableFuture<>();

        Packet.Call packet = new Packet.Call(fnId, callId, arguments);
        packets.put(new CallIdentifier(packet.callId(), packet.fnId()), null);
        sendPacket(packet);

        packets.put(callIdentifier, resFuture);
        return resFuture;
    }
}
