package com.socketry;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Function;

class DummyClass2 {
    byte[] hello(byte[] args) {
        return "Hello, World!".getBytes();
    }

    byte[] addSerialise(int a, int b) {
        return ByteBuffer.allocate(8).putInt(a).putInt(b).array();
    }
}

public class SocketryExampleServer {

    public static void main(String[] args) throws Exception {
        HashMap<String, Function<byte[], byte[]>> procedures = new HashMap<>();

        DummyClass2 dummy = new DummyClass2();
        procedures.put("hello", dummy::hello);

        SocketryServer server = new SocketryServer(60000, procedures);
        System.out.println("Server started");

        Thread handler = new Thread(server::listenLoop);
        handler.start();

        byte[] result = server.makeRemoteCall(server.getRemoteProcedureId("add"), dummy.addSerialise(4, 3), 2).get();
        int sum = ByteBuffer.wrap(result).getInt();
        System.out.println("4 + 3 = " + sum);

        handler.join();
    }
}
