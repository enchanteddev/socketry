package com.socketry.packetparser;

import java.nio.ByteBuffer;
import java.util.Arrays;


record CREPacket(byte fnId, byte callId, byte[] arguments) {
    public static CREPacket parse(byte[] data) {
        byte fnId = data[1];
        byte callId = data[2];
        int argumentsLength = data.length - 3;
        byte[] arguments = new byte[argumentsLength];
        System.arraycopy(data, 3, arguments, 0, argumentsLength);
        return new CREPacket(fnId, callId, arguments);
    }
}

public sealed interface Packet permits Packet.Call, Packet.Result, Packet.Error, Packet.Init, Packet.Accept, Packet.Ping, Packet.Pong {

    static Packet parse(byte[] data) {
        byte type = data[0];
        switch (type) {
            case PacketType.CALL:
                return Call.parse(data);
            case PacketType.RESULT:
                return Result.parse(data);
            case PacketType.ERROR:
                return Error.parse(data);
            case PacketType.INIT:
                return new Init(Arrays.copyOfRange(data, 1, data.length));
            case PacketType.ACCEPT:
                return Accept.parse(data);
            case PacketType.PING:
                return Ping.INSTANCE;
            case PacketType.PONG:
                return Pong.INSTANCE;
            default:
                throw new IllegalArgumentException("Unknown packet type: " + type);
        }
    }
    
    static ByteBuffer serialize(Packet packet) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        switch (packet) {
            case Packet.Call call:
                buffer.put(PacketType.INIT);
                buffer.put(call.fnId());
                buffer.put(call.callId());
                buffer.put(call.arguments());
                break;
            case Packet.Result result:
                buffer.put(PacketType.RESULT);
                buffer.put(result.fnId());
                buffer.put(result.callId());
                buffer.put(result.response());
                break;
            case Packet.Error error:
                buffer.put(PacketType.ERROR);
                buffer.put(error.fnId());
                buffer.put(error.callId());
                buffer.put(error.error());
                break;
            case Packet.Init init:
                buffer.put(PacketType.INIT);
                buffer.put(init.channels());
                break;
            case Packet.Accept accept:
                buffer.put(PacketType.ACCEPT);
                break;
            case Packet.Ping ping:
                buffer.put(PacketType.PING);
                break;
            case Packet.Pong pong:
                buffer.put(PacketType.PONG);
                break;
        }
        return buffer;
    }


    record Call(byte fnId, byte callId, byte[] arguments) implements Packet {
        static Call parse(byte[] data) {
            CREPacket crePacket = CREPacket.parse(data);
            return new Call(crePacket.fnId(), crePacket.callId(), crePacket.arguments());
        }
    }

    record Result(byte fnId, byte callId, byte[] response) implements Packet {
        static Result parse(byte[] data) {
            CREPacket crePacket = CREPacket.parse(data);
            return new Result(crePacket.fnId(), crePacket.callId(), crePacket.arguments());
        }
    }

    record Error(byte fnId, byte callId, byte[] error) implements Packet {
        static Error parse(byte[] data) {
            CREPacket crePacket = CREPacket.parse(data);
            return new Error(crePacket.fnId(), crePacket.callId(), crePacket.arguments());
        }
    }

    record Init(byte[] channels) implements Packet {}

    record Accept(short[] ports) implements Packet {
        static Accept parse(byte[] data) {
            // maybe a faster 0copy method exists, but i digress.
            short[] ports = new short[(data.length - 1) / 2]; // assume length is odd (even + 1 for type byte)
            for (int i = 1; i < data.length; i += 2) {
                ports[i / 2] = (short) ((data[i] << 8) | data[i + 1]);
            }
            return new Accept(ports);
        }
    }

    final class Ping implements Packet {
        public static final Ping INSTANCE = new Ping();
        private Ping() {}
    }

    final class Pong implements Packet {
        public static final Pong INSTANCE = new Pong();
        private Pong() {}
    }
}
