package com.socketry.packetparser;

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
            case 0x01:
                return Call.parse(data);
            case 0x02:
                return Result.parse(data);
            case 0x03:
                return Error.parse(data);
            case 0x04:
                return new Init(Arrays.copyOfRange(data, 1, data.length));
            case 0x05:
                return Accept.INSTANCE;
            case 0x06:
                return Ping.INSTANCE;
            case 0x07:
                return Pong.INSTANCE;
            default:
                throw new IllegalArgumentException("Unknown packet type: " + type);
        }
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

    final class Accept implements Packet {
        public static final Accept INSTANCE = new Accept();
        private Accept() {}
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
