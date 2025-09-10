package com.socketry;

import com.socketry.packetparser.PacketGenerator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


/**
 * Depending on initialization parameter
 * Either starts a server to listen on that port or
 * tries to connect to the port
 */
class Communicator {
    private SocketChannel clientChannel;

    private int port;
    private String channelName;

    public Communicator(int _port, String _channelName) throws IOException {
        port = _port;
        channelName = _channelName;
    }


    public void connect(boolean INITIATE, Selector selector, PacketGenerator packetGenerator) throws IOException {
        if (INITIATE) {
            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(true); // block till connection is established
            clientChannel.connect(new InetSocketAddress(port));
            clientChannel.configureBlocking(false);
        } else {
            // wait for other side to initiate the connection
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(true); // block till connection is established
            clientChannel = serverChannel.accept(); // blocks for new connection
            clientChannel.configureBlocking(false);
        }
        clientChannel.register(selector, SelectionKey.OP_READ, packetGenerator); // register for read
    }

    public boolean sendData(byte[] data) throws IOException {
        if (clientChannel != null) {
            clientChannel.write(ByteBuffer.wrap(data));
            return  true;
        }
        return false;
    }

    public int getPort() {
        return port;
    }

    public String getChannelName() {
        return channelName;
    }

    public void close() throws IOException {
        if (clientChannel != null) {
            clientChannel.close();
        }
    }
}
