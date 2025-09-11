package com.socketry;

import com.socketry.packetparser.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SocketryServer extends Socketry {
    private static final int LINK_PORT_START =60000;

    /**
     * Opens a server and wait for client to connect
     * @param server_port
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public SocketryServer(int server_port)
        throws IOException, InterruptedException, ExecutionException {

        SocketChannel clientInitChannel = connect(server_port).get();
        if (clientInitChannel == null) {
            throw new IOException("Unable to connect to the server");
        }
        // block to complete the handshake
        clientInitChannel.configureBlocking(true);
        Link link = new Link(clientInitChannel);

        Packet initPacket = link.getPacket().get();
        if (!(initPacket instanceof Packet.Init)) {
            throw new IllegalStateException("Expected Init packet");
        }

        // TODO : connect with given channels
        // this.setProcedures(procedures);

        Packet acceptPacket = Packet.Accept.INSTANCE;
        link.sendPacket(acceptPacket).get();

    }

    private CompletableFuture<SocketChannel> connect(int serverPort) {
        return CompletableFuture.supplyAsync(() -> {
            ServerSocketChannel serverSocketChannel = null;
            try {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(true);
                serverSocketChannel.bind(new InetSocketAddress(serverPort));
                return serverSocketChannel.accept();
            } catch (IOException e) {
                System.out.println("Message: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }
}
