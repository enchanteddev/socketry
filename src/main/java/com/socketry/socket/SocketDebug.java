package com.socketry.socket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;



public class SocketDebug implements ISocket {
    Pipe readPipe, writePipe;
    boolean connected;

    public SocketDebug() throws IOException {
        readPipe = Pipe.open();
        writePipe = Pipe.open();

        connected = false;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int r = readPipe.source().read(dst);
        return r;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return writePipe.sink().write(src);
    }

    @Override
    public SelectableChannel configureBlocking(boolean block) throws IOException {
        readPipe.source().configureBlocking(block);
        readPipe.sink().configureBlocking(block);
        writePipe.source().configureBlocking(block);
        writePipe.sink().configureBlocking(block);
        return readPipe.source();
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        connected = true;
        return true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        return readPipe.source().register(sel, ops, att);
    }

    public int readOtherSide(ByteBuffer dst) throws IOException {
        return writePipe.source().read(dst);
    }

    public int writeOtherSide(ByteBuffer src) throws IOException {
        System.out.println("Writing to other side : " + src.array().length);
        return readPipe.sink().write(src);
    }
}