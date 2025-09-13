package com.socketry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.socketry.packetparser.Packet;

public class SocketryClient extends Socketry {
    public SocketryClient(byte[] socketsPerTunnel, int serverPort,
            HashMap<String, Function<byte[], byte[]>> _procedures)
            throws IOException, InterruptedException, ExecutionException {

        this.setProcedures(_procedures);

        Link link = new Link(serverPort);
        link.configureBlocking(true);

        Packet initPacket = new Packet.Init(socketsPerTunnel);
        link.sendPacket(initPacket);

        Packet acceptPacket = link.getPacket();

        if (!(acceptPacket instanceof Packet.Accept)) {
            throw new IllegalStateException("Expected accept packet");
        }

        short[] ports = ((Packet.Accept) acceptPacket).ports();

        this.setTunnelsFromPorts(ports, socketsPerTunnel);
    }

    public void setTunnelsFromPorts(short[] ports, byte[] socketsPerTunnel) {
        ArrayList<Tunnel> tunnels = new ArrayList<>();

        byte lastSocketNum = 0;
        for (byte socketsNum : socketsPerTunnel) {
            ArrayList<Short> portsForTunnel = new ArrayList<>();
            for (int i = lastSocketNum; i < lastSocketNum + socketsNum; i++) {
                portsForTunnel.add((short) ports[i]);
            }
            lastSocketNum += socketsNum;
            try {
                Tunnel tunnel = new Tunnel(portsForTunnel.toArray(new Short[0]));
                tunnels.add(tunnel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.tunnels = tunnels.toArray(new Tunnel[0]);
    }
}
