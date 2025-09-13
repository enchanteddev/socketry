package com.socketry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Queue;

import com.socketry.packetparser.Packet;
import com.socketry.socket.ISocket;
import com.socketry.socket.SocketPipe;

public class Link {
    private final ISocket clientChannel;
    private ByteBuffer leftOverBuffer;

    private SocketPacket currentSocketPacket;

    private final Queue<Packet> packets;

    /**
     * Blocks to connect to the given port
     *
     * @param _port server port to connect to.
     */
    public Link(int _port) throws IOException {
        clientChannel = new SocketPipe();
        clientChannel.configureBlocking(true); // block till connection is established
        clientChannel.connect(new InetSocketAddress(_port));
        clientChannel.configureBlocking(false);
        packets = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }

    public Link(ISocket _connectedChannel) throws IOException {
        clientChannel = _connectedChannel;
        clientChannel.configureBlocking(false);
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
     * Puts the leftOverBuffer first then the data is read from the pipe
     * NOTE: Does **not** mark the leftOverBuffer as null. So it is responsibility of callee to mark it null when utilized
     * throws Runtime Exception if not connected
     *
     * @return
     */
    private ByteBuffer readData() throws IOException {
        if (clientChannel == null) {
            // TODO : try to re-connect first
            throw new RuntimeException("Disconnected or never connected : ");
        }

        int bufferLength = 1024;
        if (leftOverBuffer != null) {
            bufferLength = Math.max(1024, leftOverBuffer.limit() + 1024);
        }
        ByteBuffer readBuffer = ByteBuffer.allocate(bufferLength);
        if (leftOverBuffer != null) {
            readBuffer.put(leftOverBuffer);
            // This moves the pointer inside the readBuffer
        }
        int dataRead = clientChannel.read(readBuffer);
        System.out.println("readData : " + dataRead);
        if (dataRead == -1) {
            throw new RuntimeException("Disconnected or never connected : ");
        }
        System.out.println("readBuffer.position() : " + readBuffer.position());
        readBuffer.flip();
        return readBuffer.slice();
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
        ArrayList<Packet> packetsToReturn = new ArrayList<>(packets);
        packets.clear();
        return packetsToReturn;
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
        ByteBuffer buffer = readData();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data); 

        System.out.println("Read " + data.length + " bytes");

        int currDatapos = 0;
        int len = data.length;
        while (currDatapos < len) {
            if (currentSocketPacket == null) {
                // expecting a `Packet`
                int left = len - currDatapos;
                if (left < 4) {
                    // got a leftover data store it for next parse.
                    leftOverBuffer = ByteBuffer.allocate(left);
                    leftOverBuffer.put(data, currDatapos, left);
                    leftOverBuffer.flip();
                    break;
                }
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
                currentSocketPacket = null;
            }
        }
        // System.out.println("packets : " + packets);
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

        ByteBuffer socketData = ByteBuffer.allocate(packetData.length + 4);
        socketData.putInt(packetData.length);
        socketData.put(packetData);
        socketData.flip();
        System.out.println("Sending " + packetData.length + " bytes");
        System.out.println("clientChannel : " + clientChannel);

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
