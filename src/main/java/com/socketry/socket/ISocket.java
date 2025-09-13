package com.socketry.socket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface ISocket {

    int read(ByteBuffer dst) throws IOException;

    int write(ByteBuffer src) throws IOException;

    SelectableChannel configureBlocking(boolean block)
        throws IOException;

    boolean connect(SocketAddress remote) throws IOException;

    boolean isConnected();

    boolean isBlocking();

    SelectionKey register(Selector sel, int ops, Object att)
        throws ClosedChannelException;

    default SelectionKey register(Selector sel, int ops)
        throws ClosedChannelException {
        return register(sel, ops, null);
    }

}
