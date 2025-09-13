package com.socketry.socket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;

public class ServerSocketPipe implements IServerSocket {
    private final ServerSocketChannel osServerSocket;

    public ServerSocketPipe() throws IOException {
        osServerSocket = ServerSocketChannel.open();
    }


    @Override
    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        return osServerSocket.bind(local, backlog);
    }

    @Override
    public SelectableChannel configureBlocking(boolean block) throws IOException {
        return osServerSocket.configureBlocking(block);
    }

    @Override
    public ISocket accept() throws IOException {
        return new SocketPipe(osServerSocket.accept());
    }
}
