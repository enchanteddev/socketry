package com.socketry;

import com.socketry.packetparser.Packet;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


public class Socketry {
    HashMap<String, Function<byte[], byte[]>> procedures;
    String[] procedureNames;
    String[] remoteProcedureNames;

    Tunnel[] tunnels;
    Link[] links;

    private Selector selector;

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
        selector = Selector.open();

        for (Link link : links) {
            link.register(selector);
        }

        while (true) {
            ArrayList<CompletableFuture<ArrayList<Packet>>> packetFutures = new ArrayList<>();
            try {
                // Block until at least one channel is ready with some data to read
                int readyChannels = selector.select(1000); // 1 second timeout

                if (readyChannels == 0) {
                    continue; // No channels ready, continue loop
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isReadable()) {
                        Link link = (Link) key.attachment();
                        packetFutures.add(link.getPackets());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in selector loop: " + e.getMessage());
                e.printStackTrace();
            }

            CompletableFuture.allOf(packetFutures.toArray(new CompletableFuture[0])).join();
            ArrayList<Packet> packets = new ArrayList<>();
            for (CompletableFuture<ArrayList<Packet>> packetFuture : packetFutures) {
                packets.addAll(packetFuture.join());
            }
        }
    }

    byte[] handleData(byte[] data) {
        // TODO: parse the packet and call the function or respond to the checks
        return new byte[0];
    }

    byte[] handleRemoteCall(byte fnId, byte[] data) {
        Function<byte[], byte[]> procedure = procedures.get(procedureNames[fnId]);
        if (procedure == null) {
            throw new IllegalArgumentException("Unknown procedure: " + procedureNames[fnId]);
        }

        return procedure.apply(data);
    }

    public byte[] makeRemoteCall(String name, byte[] data, int tunnelId) {
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
        tnl.callFn(fnId, data, link); // TODO handle await and then wait for response

        return null; // TODO send fnId, channelId and data to socket
    }
}

