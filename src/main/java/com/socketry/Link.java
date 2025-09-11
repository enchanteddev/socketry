package com.socketry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import com.socketry.packetparser.Packet;

public class Link {
    public static int ALL_PACKETS = -1;
    private SocketChannel clientChannel;
    private ByteBuffer buffer;

    private SocketPacket currentSocketPacket;

    private Queue<Packet> packets;

    /**
     * Blocks to connect to the given port
     *
     * @param _port server port to connect to.
     */
    public Link(int _port) throws IOException {
        clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(true); // block till connection is established
        clientChannel.connect(new InetSocketAddress(_port));
        clientChannel.configureBlocking(false);
        buffer = ByteBuffer.allocate(1024);
    }

    public Link(SocketChannel _connectedChannel) throws IOException {
        clientChannel = _connectedChannel;
        clientChannel.configureBlocking(false);
        buffer = ByteBuffer.allocate(1024);
    }

    public void configureBlocking(boolean to_block) throws IOException {
        clientChannel.configureBlocking(to_block);
    }

    public void register(Selector selector) {

    }

    /**
     * reads from the channel into the buffer
     * clears the buffer before read
     * throws Runtime Exception if not connected
     *
     * @return
     */
    private int readData() throws IOException {
        if (clientChannel == null) {
            // TODO : try to re-connect first
            throw new RuntimeException("Disconnected or never connected : ");
        }
        buffer.clear();
        return clientChannel.read(buffer);
    }


    /**
     * reads and returns packets received
     * Does not reentrant over the length of the array return
     * It could also be 0
     *
     * @return
     */
    public CompletableFuture<ArrayList<Packet>> getPackets() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                readNParsePackets();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
            return new ArrayList<>(packets);
        });
    }

    /**
     * Reads an int from the given data starting at the given position
     *
     * @param data the data to read from
     * @param pos  the position to start reading from
     * @return the int read
     */
    static int readInt(byte[] data, int pos) {
        if (data.length < pos) {
            return Integer.MIN_VALUE;
        }
        int res = 0;
        int shift = 24;
        for (int i = 0; i < 4; i++) {
            res |= data[pos + i] << shift;
            shift -= 8;
        }
        return res;
    }

    public void readNParsePackets() throws IOException {
        int dataRead = readData();
        if (dataRead <= 0) { // no data to parse
            return;
        }

        byte[] data = buffer.array();
        int currDatapos = 0;
        int len = data.length;
        while (currDatapos < len) {
            if (currentSocketPacket == null) {
                // expecting a `Packet`
                // TODO: fix this code to parse the SocketPacket
                int header = readInt(data, currDatapos);
                int contentLength = readInt(data, currDatapos);
                currentSocketPacket = new SocketPacket(contentLength, new ByteArrayOutputStream());
            }
            // check how much data is still to be read
            // and how much can be read
            int to_read = Math.min(currentSocketPacket.contentLength, data.length);
            currentSocketPacket.content.write(data, currDatapos, to_read);
            currentSocketPacket.contentLength -= to_read;
            currDatapos += to_read;
            if (currentSocketPacket.contentLength == 0) {
                packets.add(Packet.parse(currentSocketPacket.content.toByteArray()));
            }
        }
    }

    public CompletableFuture<Packet> getPacket() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                readNParsePackets();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return null;
            }
            return packets.poll();
        });
    }

    public CompletableFuture<Boolean> sendPacket(Packet packet) {
        return CompletableFuture.supplyAsync(() -> {
            byte[] data = null;
            SocketPacket spacket = SocketPacket.fromPacket(packet);
            if (spacket != null) {
                data = spacket.content.toByteArray();
            }
            if (clientChannel != null && data != null) {
                try {
                    clientChannel.write(ByteBuffer.wrap(data));
                    return true;
                } catch (IOException e) {
                    System.out.println("Error while writing" + e.getMessage());
                    e.printStackTrace();
                }
            }
            return false;
        });
    }
}

class SocketPacket {
    int contentLength;
    ByteArrayOutputStream content;

    public SocketPacket(int contentLength, ByteArrayOutputStream content) {
        this.contentLength = contentLength;
        this.content = content;
    }

    public static SocketPacket fromPacket(Packet packet) {
        return null;
    }
}
