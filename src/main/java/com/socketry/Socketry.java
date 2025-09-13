package com.socketry;

import com.socketry.packetparser.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        for (String procedureName : procedureNames) {
            buffer.put(procedureName.getBytes());
            buffer.put((byte) 0);
        }
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
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

    public void getRemoteProcedureNames() throws IOException, InterruptedException, ExecutionException {
        byte[] initResponse = makeRemoteCall((byte) 0, new byte[0], 0).get();

        ArrayList<String> remoteProcedureNamesList = new ArrayList<>();
        StringBuilder currentName = new StringBuilder();
        for (byte b : initResponse) {
            if (b == 0) {
                remoteProcedureNamesList.add(currentName.toString());
                currentName = new StringBuilder();
            } else {
                currentName.append((char) b);
            }
        }
        remoteProcedureNames = remoteProcedureNamesList.toArray(new String[0]);
    }

    public void listenLoop() {
        try {
            startListening();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startListening() throws IOException {
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
            case Packet.Ping ignored -> {
                tunnel.sendPacket(Packet.Pong.INSTANCE);
            }
            default -> // just log for now
                System.err.println("Unhandled packet: " + packet);
        }
    }

    byte[] handleRemoteCall(byte fnId, byte[] data) {
        Function<byte[], byte[]> procedure = procedures.get(procedureNames[fnId]);
        if (procedure == null) {
            throw new IllegalArgumentException("Unknown procedure: " + procedureNames[fnId]);
        }

        return procedure.apply(data);
    }

    public byte getRemoteProcedureId(String name) {
        if (remoteProcedureNames == null) {
            System.out.println("Fetching remote procedure names...");
            try {
                getRemoteProcedureNames();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (byte i = 0; i < remoteProcedureNames.length; i++) {
            if (remoteProcedureNames[i].equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown procedure: " + name);
    }

    public CompletableFuture<byte[]> makeRemoteCall(byte fnId, byte[] data, int tunnelId) throws InterruptedException {
        if (tunnelId < 0 || tunnelId >= tunnels.length) {
            throw new IllegalArgumentException("Invalid channelId: " + tunnelId);
        }

        Tunnel tnl = tunnels[tunnelId];
        return tnl.callFn(fnId, data);
    }
}

