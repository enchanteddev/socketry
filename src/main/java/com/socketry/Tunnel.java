package com.socketry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import com.socketry.packetparser.Packet;

record CallIdentifier(byte callId, byte fnId) {
    @Override
    public final int hashCode() {
        return callId << 8 | fnId;
    }
}

public class Tunnel {
    HashMap<CallIdentifier, CompletableFuture<byte[]>> packets;
    Queue<Packet> packetQueue; // worst case scenario is number of packets in transit > 255

    ArrayList<Integer> LinkIds;

    public Tunnel(ArrayList<Integer> LinkIds) {
        this.LinkIds = LinkIds;
    }

    public void feedPacket(Packet packet) {
        switch (packet) {
            case Packet.Result resPacket -> {
                byte[] result = resPacket.response();
                CallIdentifier callIdentifier = new CallIdentifier(resPacket.callId(), resPacket.fnId());
                CompletableFuture<byte[]> resFuture = packets.get(callIdentifier);
                if (resFuture != null) {
                    resFuture.complete(result);
                }
            }
            default -> {
                // TODO: match the other types
            }
        }
    }

    public int selectLink() {
        return 0;
        // TODO return a link at random
    }

    byte assignCallId() {
        return 0;
        // TODO
    }

    public CompletableFuture<byte[]> callFn(byte fnId, byte[] arguments, Link link) {
        byte callId = assignCallId();
        CallIdentifier callIdentifier = new CallIdentifier(callId, fnId);

        CompletableFuture<byte[]> resFuture = new CompletableFuture<>();

        Packet.Call packet = new Packet.Call(fnId, assignCallId(), arguments);
        packets.put(new CallIdentifier(packet.callId(), packet.fnId()), null);
        link.sendPacket(packet);

        packets.put(callIdentifier, resFuture);
        return resFuture;
    }
}
