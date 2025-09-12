package com.socketry;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

class DummyClass {
    int add(int a, int b) {
        return a + b;
    }
    
    byte[] addWrapper(byte[] args) {
        if (args.length != 8) {
            throw new IllegalArgumentException("Expected 8 bytes");
        }
        int a = ByteBuffer.wrap(Arrays.copyOfRange(args, 0, 4)).getInt();
        int b = ByteBuffer.wrap(Arrays.copyOfRange(args, 4, 8)).getInt();
        int result = add(a, b);
        return ByteBuffer.allocate(4).putInt(result).array();
    }
}

public class SocketryExampleClient {

    public static void main(String[] args) throws Exception {
        HashMap<String, Function<byte[], byte[]>> procedures = new HashMap<>();
        
        DummyClass dummy = new DummyClass();
        procedures.put("add", dummy::addWrapper);
        
        SocketryClient client = new SocketryClient(new byte[] {1, 1, 1}, 60000, procedures);
        
        Thread handler = new Thread(client::listenLoop);
        handler.start();

        client.makeRemoteCall(client.getRemoteProcedureId("hello"), new byte[0], 1).get();
        
        handler.join();
    }
}
