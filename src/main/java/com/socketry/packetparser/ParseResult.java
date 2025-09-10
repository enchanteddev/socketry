package com.socketry.packetparser;

public record ParseResult(Packet packet, int bytesConsumed) {}
