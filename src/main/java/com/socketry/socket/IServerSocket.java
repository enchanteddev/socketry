package com.socketry.socket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;

public interface IServerSocket {
    default ServerSocketChannel bind(SocketAddress local)
        throws IOException {
        return bind(local, 0);
    }

    ServerSocketChannel bind(SocketAddress local, int backlog)
        throws IOException;

    SelectableChannel configureBlocking(boolean block)
        throws IOException;

    ISocket accept() throws IOException;

}
