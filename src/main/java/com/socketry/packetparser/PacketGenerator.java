package com.socketry.packetparser;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class PacketGenerator {
    static int MAGIC_IDENTIFIER=777431;
    private SocketPacket currentSocketPacket;
    
    /**
     * Reads an int from the given data starting at the given position
     * @param data the data to read from
     * @param pos the position to start reading from
     * @return the int read
     */
    static int readInt(byte[] data, int pos) {
        if (data.length < pos) {
            return Integer.MIN_VALUE;
        }
        int res = 0;
        int shift = 24;
        for (int i  = 0; i < 4; i ++) {
            res |= data[pos + i] << shift;
            shift -= 8;
        }
        return res;
    }

    public ArrayList<Packet> getPackets(byte[] data) {
        int currDatapos=0;
        int len = data.length;
        ArrayList<Packet> parsedPackets = new ArrayList<>();
        while (currDatapos < len) {
            if (currentSocketPacket == null) {
                // expecting a `Packet`
                if (data.length < 8)  {
                    // wait to get both magic header and content length
                    break;
                }
                int header= readInt(data, currDatapos);
                currDatapos += 4;
                if (header != MAGIC_IDENTIFIER) {
                    // corrupted data
                    continue;
                }
                int contentLength = readInt(data, currDatapos);
                currDatapos += 4;
                currentSocketPacket = new SocketPacket(MAGIC_IDENTIFIER, contentLength, new ByteArrayOutputStream());
            }
            // check how much data is still to be read
            // and how much can be read
            int to_read = Math.min(currentSocketPacket.contentLength, data.length);
            currentSocketPacket.content.write(data, currDatapos, to_read);
            currentSocketPacket.contentLength -= to_read;
            currDatapos += to_read;
            if (currentSocketPacket.contentLength == 0) {
                parsedPackets.add(Packet.parse(currentSocketPacket.content.toByteArray()));
            }
        }
        return parsedPackets;
    }
}

class SocketPacket {
    int MAGIC;
    int contentLength;
    ByteArrayOutputStream content;

    public SocketPacket(int MAGIC, int contentLength, ByteArrayOutputStream content) {
        this.MAGIC = MAGIC;
        this.contentLength = contentLength;
        this.content = content;
    }

}
