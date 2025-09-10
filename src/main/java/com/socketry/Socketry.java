package com.socketry;

import com.socketry.packetparser.Packet;
import com.socketry.packetparser.PacketGenerator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Socketry {
    byte[] socketsPerChannel;
    HashMap<String, Function<byte[], byte[]>> procedures;
    String[] procedureNames;

    String[] remoteProcedureNames;
    HashMap<String, ArrayList<Communicator>> channelCommunicators;
    private Selector selector;
    private boolean initiateConnections;


    byte[] getProcedures() {
        return null; // TODO
    }

    public Socketry(byte[] socketsPerChannel, String[] channelNames, int start_port, boolean _initiateConnections,
            HashMap<String, Function<byte[], byte[]>> procedures) throws IOException {

        initiateConnections = _initiateConnections;
        if (socketsPerChannel.length != channelNames.length) {
            return;
        }
        channelCommunicators = new HashMap<>();
        for (int i = 0; i < socketsPerChannel.length; i ++) {
            int len = socketsPerChannel[i];
            ArrayList<Communicator> communicators = new ArrayList<>();
            for (int j = 0; j < len; j ++) {
                communicators.add(new Communicator(start_port ++, channelNames[i]));
            }
            channelCommunicators.put(channelNames[i], communicators);
        }

        this.socketsPerChannel = socketsPerChannel;
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
        ByteBuffer buffer = ByteBuffer.allocate(8192);

        // initiate the connections
        for (Map.Entry<String, ArrayList<Communicator>> entry : channelCommunicators.entrySet()) {
            ArrayList<Communicator> communicators = entry.getValue();
            for (Communicator communicator : communicators) {
                communicator.connect(initiateConnections, selector, new PacketGenerator());
            }
        }

        while (true) {
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
                        handleRead(key, buffer);
                    }

                    keyIterator.remove();
                }
            } catch (IOException e) {
                System.err.println("Error in selector loop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }



    CompletableFuture<Void> handleRead(SelectionKey key, ByteBuffer buffer) throws IOException {
        return CompletableFuture.runAsync(() -> {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            PacketGenerator packetGenerator = (PacketGenerator) key.attachment();
            buffer.clear();
            int bytesRead = 0;
            try {
                bytesRead = clientChannel.read(buffer);
            } catch (IOException e) {
                System.err.println("Error in selector loop: " + e.getMessage());
                e.printStackTrace();
            }
            if (bytesRead > 0) {
                ArrayList<Packet> packets = packetGenerator.getPackets(buffer.array());
                for (Packet packet : packets) {
                    switch (packet) {
                        case Packet.Call call -> {

                        }
                    }
                }
            }
        });
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

    public byte[] makeRemoteCall(String name, byte[] data, int channelId) {
        int fnId = -1;
        for (int i = 0; i < remoteProcedureNames.length; i++) {
            if (remoteProcedureNames[i].equals(name)) {
                fnId = i;
                break;
            }
        }
        if (fnId == -1) {
            throw new IllegalArgumentException("Unknown procedure: " + name);
        }

        if (channelId < 0 || channelId >= socketsPerChannel.length) {
            throw new IllegalArgumentException("Invalid channelId: " + channelId);
        }

        if (socketsPerChannel[channelId] == 0) {
            throw new IllegalArgumentException("No sockets available for channel: " + channelId);
        }

        return null; // TODO send fnId, channelId and data to socket
    }
}