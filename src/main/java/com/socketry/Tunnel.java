package com.socketry;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.socketry.packetparser.Packet;
import com.socketry.socket.ISocket;

import static java.lang.Thread.sleep;

record CallIdentifier(byte callId, byte fnId) {
    @Override
    public int hashCode() {
        return callId << 8 | fnId;
    }
}

public class Tunnel {
    private static final byte NO_CALL_IDS_AVAILABLE = 127;
    Map<CallIdentifier, CompletableFuture<byte[]>> packets;

    ArrayList<Link> Links;
    Selector selector;

    private void initialize() throws IOException {
        this.selector = Selector.open();
        this.packets = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Creates a tunnel with given number of links.
     * Also creates a selector and registers all the created links to the selector
     *
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
     *
     * @param sockets :
     * @throws IOException
     */
    public Tunnel(ISocket[] sockets) throws IOException {
        initialize();
        this.Links = new ArrayList<>();
        for (ISocket socketChannel : sockets) {
            Link link = new Link(socketChannel);
            link.register(selector);
            Links.add(link);
        }
    }

    private Packet feedPacket(Packet packet) {
//        System.out.println("Consuming Packet : " + packet);
        switch (packet) {
            case Packet.Result resPacket -> {
                byte[] result = resPacket.response();
                CallIdentifier callIdentifier = new CallIdentifier(resPacket.callId(), resPacket.fnId());
                CompletableFuture<byte[]> resFuture = packets.get(callIdentifier);
                if (resFuture != null) {
                    // System.out.println("Completing Future : " + resFuture);
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

    /**
     * Creates a callIdentifier and creates an entry in packets map
     * @param fnId
     * @return
     */
    synchronized CallIdentifier getCallIdentifier(byte fnId) {
        byte callId = NO_CALL_IDS_AVAILABLE;
        while (true) {
            byte i = -127;
            for (; i < 127; i++) {
                if (!packets.containsKey(new CallIdentifier(i, fnId))) {
                    callId = i;
                    break;
                }
            }
            if (callId != NO_CALL_IDS_AVAILABLE) {
                break;
            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Error Message : " + e.getMessage());
                e.printStackTrace();
            }
        }
        CallIdentifier callIdentifier = new CallIdentifier(callId, fnId);
        packets.put(callIdentifier, null);
        return callIdentifier;
    }

    /**
     * send the packet via any of the link of the Tunnel
     *
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

//            System.out.println("readyChannels : " + readyChannels);

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove(); // drain the selected-key set
                if (!key.isValid()) {
                    continue;
                }

                if (key.isReadable()) {
                    Link link = (Link) key.attachment();
                    packets.addAll(link.getPackets());
                }
                // System.out.println("packets : " + packets);
            }

        } catch (IOException e) {
            System.err.println("Error in selector loop: " + e.getMessage());
            e.printStackTrace();
        }
//        System.out.println("packets : " + packets);
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

    public CompletableFuture<byte[]> callFn(byte fnId, byte[] arguments) throws InterruptedException {

        CallIdentifier callIdentifier = getCallIdentifier(fnId);

        CompletableFuture<byte[]> resFuture = new CompletableFuture<>();
        Packet.Call packet = new Packet.Call(fnId, callIdentifier.callId(), arguments);
        sendPacket(packet);

        packets.put(callIdentifier, resFuture);
        return resFuture;
    }
}
