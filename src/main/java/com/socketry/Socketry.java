package com.socketry;

import com.socketry.packetparser.Packet;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


/**
 * Just provides the required methods
 */
public abstract class Socketry {
    HashMap<String, Function<byte[], byte[]>> procedures;
    String[] procedureNames;
    String[] remoteProcedureNames;

    Tunnel[] tunnels;

    byte[] getProcedures() {
        return null; // TODO use JSON here
    }

    public void setProcedures(
        HashMap<String, Function<byte[], byte[]>> procedures) {
        this.procedures = procedures;
        this.procedures.put("getProcedures", i -> this.getProcedures());

        this.procedureNames = new String[procedures.size()];
        this.procedureNames[0] = "getProcedures";

        int index = 1;
        for (String key : procedures.keySet()) {
            if (!key.equals("getProcedures")) {
                this.procedureNames[index++] = key;
            }
        }
    }

    public void startListening() throws IOException {
        Selector selector = Selector.open();

        while (true) {
            // listen to each tunnel
            // since each are configured in non-blocking mode
            // they just returns back almost instantly
            for (Tunnel tunnel : tunnels) {
                ArrayList<Packet> unhandledPackets = tunnel.listen();
                unhandledPackets.forEach(packet -> {
                    handlePacket(packet, tunnel);
                });
            }
        }
    }

    /**
     * Handles unhandled packets from the tunnel
     *
     * @param packet
     */
    public void handlePacket(Packet packet, Tunnel tunnel) {
        switch (packet) {
            case Packet.Call callPacket -> {
                /*
                  handles the remote call and returns the result
                 */
                Packet responsePacket;
                try {
                    byte[] response = handleRemoteCall(callPacket.fnId(), callPacket.arguments());
                    responsePacket = new Packet.Result(callPacket.fnId(), callPacket.callId(), response);
                } catch (Exception e) {
                    responsePacket =
                        new Packet.Error(callPacket.fnId(), callPacket.callId(), e.getMessage().getBytes());
                }
                tunnel.sendPacket(responsePacket);
            }
            case Packet.Ping pingPacket -> {
                tunnel.sendPacket(Packet.Pong.INSTANCE);
            }
            default -> {
                // just log for now
                System.err.println("Unhandled packet: " + packet);
            }
        }
    }

    byte[] handleRemoteCall(byte fnId, byte[] data) {
        Function<byte[], byte[]> procedure = procedures.get(procedureNames[fnId]);
        if (procedure == null) {
            throw new IllegalArgumentException("Unknown procedure: " + procedureNames[fnId]);
        }

        return procedure.apply(data);
    }

    public CompletableFuture<byte[]> makeRemoteCall(String name, byte[] data, int tunnelId) {
        byte fnId = -1;
        for (byte i = 0; i < remoteProcedureNames.length; i++) {
            if (remoteProcedureNames[i].equals(name)) {
                fnId = i;
                break;
            }
        }
        if (fnId == -1) {
            throw new IllegalArgumentException("Unknown procedure: " + name);
        }

        if (tunnelId < 0 || tunnelId >= tunnels.length) {
            throw new IllegalArgumentException("Invalid channelId: " + tunnelId);
        }

        Tunnel tnl = tunnels[tunnelId];
        return tnl.callFn(fnId, data);
    }
}

