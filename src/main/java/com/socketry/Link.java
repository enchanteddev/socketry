package com.socketry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Queue;

import com.socketry.packetparser.Packet;

public class Link {
    private final SocketChannel clientChannel;
    private final ByteBuffer buffer;

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
        packets = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }

    public Link(SocketChannel _connectedChannel) throws IOException {
        clientChannel = _connectedChannel;
        clientChannel.configureBlocking(false);
        buffer = ByteBuffer.allocate(1024);
        packets = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }

    public void configureBlocking(boolean to_block) throws IOException {
        clientChannel.configureBlocking(to_block);
    }

    public void register(Selector selector) throws ClosedChannelException {
        if (clientChannel != null) {
            clientChannel.register(selector, SelectionKey.OP_READ, this);
        }
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
        byte[] leftOverData = buffer.array();
        // only take in so that
        ByteBuffer readBuffer = ByteBuffer.allocate(1024 - leftOverData.length);
        int dataRead = clientChannel.read(readBuffer);
        buffer.clear();
        buffer.put(leftOverData);
        if (dataRead > 0) {
            buffer.put(readBuffer);
        }
        buffer.flip();
        return dataRead;
    }

    /**
     * reads and returns packets received
     * Does not reentrant over the length of the array return
     * It could also be 0
     *
     * @return
     */
    public ArrayList<Packet> getPackets() {
        try {
            readNParsePackets();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
        return new ArrayList<>(packets);
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
        System.out.println("Read " + dataRead + " bytes");
        if (dataRead <= 0) { // no data to parse
            return;
        }

        byte[] data = buffer.array();
        int currDatapos = 0;
        int len = data.length;
        while (currDatapos < len) {
            if (currentSocketPacket == null) {
                // expecting a `Packet`
                int contentLength = readInt(data, currDatapos);
                currentSocketPacket = new SocketPacket(contentLength, new ByteArrayOutputStream());
                currDatapos += 4;
            }
            // check how much data is still to be read
            // and how much can be read
            int to_read = Math.min(currentSocketPacket.bytesLeft, data.length);
            currentSocketPacket.content.write(data, currDatapos, to_read);
            currentSocketPacket.bytesLeft -= to_read;
            currDatapos += to_read;
            if (currentSocketPacket.bytesLeft == 0) {
                packets.add(Packet.parse(currentSocketPacket.content.toByteArray()));
            }
        }
    }

    public Packet getPacket() {
        try {
            readNParsePackets();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
        return packets.poll();
    }

    public boolean sendPacket(Packet packet) {
        byte[] packetData = Packet.serialize(packet).array();

        ByteBuffer socketData = ByteBuffer.allocate(1024);
        socketData.putInt(packetData.length);
        System.out.println("Sending " + packetData.length + " bytes");
        socketData.put(packetData);
        socketData.flip();

        if (clientChannel != null) {
            try {
                clientChannel.write(socketData);
                return true;
            } catch (IOException e) {
                System.out.println("Error while writing" + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }
}

class SocketPacket {
    int bytesLeft;
    ByteArrayOutputStream content;

    public SocketPacket(int contentLength, ByteArrayOutputStream content) {
        this.bytesLeft = contentLength;
        this.content = content;
    }
}
