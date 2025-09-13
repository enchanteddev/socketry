package com.socketry.socket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SocketPipe implements ISocket {
    private final SocketChannel osSocket;

    public SocketPipe() throws IOException {
        osSocket = SocketChannel.open();
    }

    public SocketPipe(SocketChannel _socketChannel) throws IOException {
        osSocket = _socketChannel;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return osSocket.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return osSocket.read(dsts, offset, length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return osSocket.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return osSocket.write(srcs, offset, length);
    }

    @Override
    public SelectableChannel configureBlocking(boolean block) throws IOException {
        return osSocket.configureBlocking(block);
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        return osSocket.connect(remote);
    }

    @Override
    public boolean isConnected() {
        return osSocket.isConnected();
    }

    @Override
    public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        return osSocket.register(sel, ops, att);
    }
}
