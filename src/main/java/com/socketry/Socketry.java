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
    Link[] links;
    HashMap<Link, Tunnel> linkToTunnel;

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

        for (Link link : links) {
            link.register(selector);
        }

        while (true) {
            HashMap<Tunnel, ArrayList<ArrayList<Packet>>> packetFutures = new HashMap();
            try {
                // Block until at least one channel is ready with some data to read
                int readyChannels = selector.select(1000); // 1 second timeout

                if (readyChannels == 0) {
                    continue; // No channels ready, continue loop
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();

                for (SelectionKey key : selectedKeys) {
                    if (key.isReadable()) {
                        Link link = (Link) key.attachment();
                        Tunnel tunnel = linkToTunnel.get(link);
                        if (tunnel == null) {
                            System.err.println("Error: tunnel not found for link");
                            continue;
                        }
                        ArrayList<ArrayList<Packet>> packetFuturesForTunnel =
                            packetFutures.computeIfAbsent(tunnel, k -> new ArrayList<>());
                        packetFuturesForTunnel.add(link.getPackets());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in selector loop: " + e.getMessage());
                e.printStackTrace();
            }

            CompletableFuture.allOf(packetFutures.values().toArray(new CompletableFuture[0])).join();
            packetFutures.forEach((tunnel, tunnelPackets) -> {
                tunnelPackets.forEach(packetFuture -> {
                    ArrayList<Packet> packetsForTunnel = packetFuture;
                    packetsForTunnel.forEach(tunnel::feedPacket);
                });
            });

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
        int linkId = tnl.selectLink();
        Link link = links[linkId];
        return tnl.callFn(fnId, data, link);
    }
}

