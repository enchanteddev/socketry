package com.socketry;

import com.socketry.packetparser.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class SocketryServer extends Socketry {
    private static final short LINK_PORT_START = (short) 10000;

    /**
     * Opens a server and wait for client to connect
     * @param serverPort
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public SocketryServer(int serverPort, HashMap<String, Function<byte[], byte[]>> _procedures)
        throws IOException, InterruptedException, ExecutionException {

        this.setProcedures(_procedures);
        
        ServerSocketChannel initServer = startAt(serverPort);
        if (initServer == null) {
            throw new IOException("Unable to start server");
        }
        initServer.configureBlocking(true);

        SocketChannel clientInitChannel = initServer.accept();
        
        // block to complete the handshake
        Link link = new Link(clientInitChannel);
        link.configureBlocking(true);

        Packet initPacket = link.getPacket();

        if (!(initPacket instanceof Packet.Init)) {
            throw new IllegalStateException("Expected Init packet" + initPacket);
        }

        byte[] socketsPerTunnel = ((Packet.Init) initPacket).channels();

        ArrayList<ServerSocketChannel> serverSockets = new ArrayList<>();
        ArrayList<Short> ports = new ArrayList<>();
        short current_port = LINK_PORT_START;

        for (byte socketNum : socketsPerTunnel) {
            for (short i = 0; i < socketNum; i++) {
                while (true) {
                    current_port++;
                    ServerSocketChannel socketChannel = startAt(current_port);
                    if (socketChannel == null) {
                        continue;
                    }
                    serverSockets.add(socketChannel);
                    ports.add(current_port);
                    break;
                }
            }
        }

        short[] portsArray = new short[ports.size()];
        for (int i = 0; i < ports.size(); i++) {
            portsArray[i] = ports.get(i);
        }

        Packet acceptPacket = new Packet.Accept(portsArray);
        link.sendPacket(acceptPacket);

        ArrayList<SocketChannel> clientSockets = new ArrayList<>();

        for (ServerSocketChannel serverSocket : serverSockets) {
            serverSocket.configureBlocking(true);
            SocketChannel clientChannel = serverSocket.accept();
            clientSockets.add(clientChannel);
        }

        this.setTunnelsFromSockets(clientSockets.toArray(new SocketChannel[0]), socketsPerTunnel);
        this.getRemoteProcedureNames();
    }

    public void setTunnelsFromSockets(SocketChannel[] sockets, byte[] socketsPerTunnel) {
        ArrayList<Tunnel> tunnels = new ArrayList<>();

        byte lastSocketNum = 0;
        for (byte socketsNum: socketsPerTunnel) {
            ArrayList<SocketChannel> socketsForTunnel = new ArrayList<>();
            for (int i = lastSocketNum; i < lastSocketNum + socketsNum; i++) {
                socketsForTunnel.add(sockets[i]);
            }
            lastSocketNum += socketsNum;
            try {
                Tunnel tunnel = new Tunnel(socketsForTunnel.toArray(new SocketChannel[0]));
                tunnels.add(tunnel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.tunnels = tunnels.toArray(new Tunnel[0]);
    }

    private ServerSocketChannel startAt(int serverPort) {
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(serverPort));
            return serverSocketChannel;
        } catch (IOException e) {
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
