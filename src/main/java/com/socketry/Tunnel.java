package com.socketry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

import com.socketry.packetparser.Packet;

record CallIdentifier(byte callId, byte fnId) {
}

public class Tunnel {
    HashMap<CallIdentifier, Object> packets; // TODO replace Object with something like CompleteableFuture
    Queue<Packet> packetQueue; // worst case scenario is number of packets in transit > 255

    ArrayList<Integer> LinkIds;

    public Tunnel(ArrayList<Integer> LinkIds) {
        this.LinkIds = LinkIds;
    }

    public void feedPacket(Packet packet) {
        // TODO
    }

    public int selectLink() {
        return 0;
        // TODO return a link at random
    }

    byte assignCallId() {
        return 0;
        // TODO
    }

    public void callFn(byte fnId, byte[] arguments, Link link) {
        Packet.Call packet = new Packet.Call(fnId, assignCallId(), arguments);
        packets.put(new CallIdentifier(packet.callId(), packet.fnId()), null);
        link.sendPacket(packet);
    }
}
