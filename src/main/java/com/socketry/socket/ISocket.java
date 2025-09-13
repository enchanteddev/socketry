package com.socketry.socket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface ISocket {

    int read(ByteBuffer dst) throws IOException;

    long read(ByteBuffer[] dsts, int offset, int length)
        throws IOException;

    default long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    int write(ByteBuffer src) throws IOException;

    long write(ByteBuffer[] srcs, int offset, int length)
        throws IOException;

    default long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    SelectableChannel configureBlocking(boolean block)
        throws IOException;

    boolean connect(SocketAddress remote) throws IOException;

    boolean isConnected();

    SelectionKey register(Selector sel, int ops, Object att)
        throws ClosedChannelException;

    default SelectionKey register(Selector sel, int ops)
        throws ClosedChannelException {
        return register(sel, ops, null);
    }

}
