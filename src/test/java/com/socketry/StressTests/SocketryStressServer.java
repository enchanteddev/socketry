package com.socketry.StressTests;


import com.socketry.Socketry;
import com.socketry.SocketryServer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

class StressServerClass1 implements ISocketryStress {

    byte[] hello(byte[] args) {
        return "Hello, World!".getBytes();
    }

    byte[] time(byte[] args) {
        long curr = System.nanoTime();
        return ByteBuffer.allocate(8).putLong(curr).array();
    }

    byte[] addSerialise(int a, int b) {
        return ByteBuffer.allocate(8).putInt(a).putInt(b).array();
    }

    ////////////
    /// Serializers
    ////////////
    

    void assertAdd(Socketry socketry, int tunnelId) throws InterruptedException, ExecutionException {
        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.CLIENT_FUNC_ADD);
        int num1 = (int) (Math.random() * 100);
        int num2 = (int) (Math.random() * 100);
        byte[] result = socketry.makeRemoteCall(fnId, addSerialise(num1, num2), tunnelId).get();
        int sum = ByteBuffer.wrap(result).getInt();
        assertEquals(num1 + num2, sum);
    }

    long readLong(byte[] bytes) {
        // get the first 8 bytes without extra allocation
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getLong();
    }

    double checkTime(Socketry socketry, int tunnelId, int i) throws InterruptedException, ExecutionException {
        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.CLIENT_FUNC_TIME);
        long start = System.nanoTime();
        byte[] endbytes = socketry.makeRemoteCall(fnId, new byte[0], tunnelId).get();
        long finish = System.nanoTime();
        long end = readLong(endbytes);
        double duration = (end - start) / 1_000_000.0;
        double duration2 = (finish - start) / 1_000_000.0;
        return duration2;
//        System.out.println("Server" + i + " --- Received time : " + duration + " ms " + "Round Trip time : " + duration2 + " ms = " + "Diff : " + (duration2 - duration)  + " ms");
    }

    @Override
    public void startFunctionality(Socketry socketry, int sleepDur, int times, int tunnelId) {
         int i = 0;
         double total = 0;
        System.out.println("Server Class 1 start: " + tunnelId);
        while (times -- > 0) {
            try {
                // assertAdd(socketry, tunnelId);

                total  += checkTime(socketry, tunnelId, i ++);
//                System.out.println("ServerClass1 ran" + i ++);
            } catch (Exception e) {
                System.out.println("Error : " + e.getMessage());
                e.printStackTrace();
                assert (false);
            }
        }
        System.out.println("Server Class1 Ran: " + total + "Average time: " + (total / i)); 
    }
}
/**
 * Simulate complex Functions
 */
class StressServerComplexFunctions implements ISocketryStress {
    long addMultiplied(int a, int b, int c,int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v, int w, int x, int y, int z) {
        return (long)a + b + c + d + e + f + g + h + i + j + k + l + m + n + o + p + q + r + s + t + u + v + w + x + y + z;
    }

    byte[] addMultipliedWrapper(byte[] args) {
        ByteBuffer buffer = ByteBuffer.wrap(args);
        int a = buffer.getInt();
        int b = buffer.getInt();
        int c = buffer.getInt();
        int d = buffer.getInt();
        int e = buffer.getInt();
        int f = buffer.getInt();
        int g = buffer.getInt();
        int h = buffer.getInt();
        int i = buffer.getInt();
        int j = buffer.getInt();
        int k = buffer.getInt();
        int l = buffer.getInt();
        int m = buffer.getInt();
        int n = buffer.getInt();
        int o = buffer.getInt();
        int p = buffer.getInt();
        int q = buffer.getInt();
        int r = buffer.getInt();
        int s = buffer.getInt();
        int t = buffer.getInt();
        int u = buffer.getInt();
        int v = buffer.getInt();
        int w = buffer.getInt();
        int x = buffer.getInt();
        int y = buffer.getInt();
        int z = buffer.getInt();
        long result = addMultiplied(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z);
        return ByteBuffer.allocate(8).putLong(result).array();
    }

    short[][][] addFilter(short[][][] a, short[][][] b) {
        short[][][] result = new short[a.length][a[0].length][a[0][0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                for (int k = 0; k < a[0][0].length; k++) {
                    result[i][j][k] = (short)(a[i][j][k] + b[i][j][k]);
                }
            }
        }
        return result;
    }

    byte[] bigDataExchange(byte[] args) {
        return args;
    }

    short[][][] deserializeImage(byte[] image) {
        ByteBuffer buffer = ByteBuffer.wrap(image);
        int length = buffer.getInt();
        int width = buffer.getInt();
        int height = buffer.getInt();
        short[][][] result = new short[length][width][height];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    result[i][j][k] = buffer.getShort();
                }
            }
        }
        return result;
    }
    
    byte[] addFilterWrapper(byte[] args) {
        short[][][] a = deserializeImage(args);

        int lena = a.length * a[0].length * a[0][0].length * 2 + 12;
        short[][][] b = deserializeImage(Arrays.copyOfRange(args,lena, args.length));
        
        short[][][] result = addFilter(a, b);
        return serialiseImage(result);
    }

    byte[] serialiseImage(short[][][] image) {
        ByteBuffer buffer = ByteBuffer.allocate(image.length * image[0].length * image[0][0].length * 2 + 12);
        buffer.putInt(image.length);
        buffer.putInt(image[0].length); 
        buffer.putInt(image[0][0].length);
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                for (int k = 0; k < image[0][0].length; k++) {
                    short val = image[i][j][k];
                    buffer.putShort(val);
                }
            }
        }
        return buffer.array();
    }

    void assertUIUpdate(Socketry socketry, int tunnelId) throws InterruptedException, ExecutionException {
        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.CLIENT_FUNC_UPDATE_SCREEN_UI);
        short[][][] image = new short[3][3][3];
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                for (int k = 0; k < image[0][0].length; k++) {
                    image[i][j][k] = (short) (Math.random() * 100);
                }
            }
        }
        byte[] result = socketry.makeRemoteCall(fnId, serialiseImage(image), tunnelId).get();
        assertEquals(0 , result.length);
    }

    @Override
    public void startFunctionality(Socketry socketry, int sleepDur, int times, int tunnelId) {
        int i  = 0;
        System.out.println("Server Complex : " + tunnelId);
        while (times -- > 0) {
            try {
                assertUIUpdate(socketry, tunnelId);
//                System.out.println("ServerComplex Ran: " + i ++);
            } catch (Exception e) {
                System.out.println("Error : " + e.getMessage());
                e.printStackTrace();
                assert (false);
            }
        }
    }
}

public class SocketryStressServer {

    public static void start(int times) throws Exception {
        HashMap<String, Function<byte[], byte[]>> procedures = new HashMap<>();

        StressServerClass1 dummy = new StressServerClass1();
        procedures.put(SocketryStressFunc.SERVER_FUNC_HELLO, dummy::hello);
        procedures.put(SocketryStressFunc.SERVER_FUNC_TIME, dummy::time);

        StressServerComplexFunctions dummy2 = new StressServerComplexFunctions();
        procedures.put(SocketryStressFunc.SERVER_ADDMULTIPLIED, dummy2::addMultipliedWrapper);
        procedures.put(SocketryStressFunc.SERVER_ADDFILTER, dummy2::addFilterWrapper);
        procedures.put(SocketryStressFunc.SERVER_BIGDATAEXCHANGE, dummy2::bigDataExchange);

        SocketryServer server = new SocketryServer(60000, procedures);
        System.out.println("Server started");

        Thread handler = new Thread(server::listenLoop);
        handler.start();

        byte[] result = server.makeRemoteCall(server.getRemoteProcedureId("add"), dummy.addSerialise(4, 3), 2).get();
        int sum = ByteBuffer.wrap(result).getInt();
        System.out.println("4 + 3 = " + sum);

        ArrayList<ISocketryStress> stressClients = new ArrayList<>();

        stressClients.add(dummy);
//        stressClients.add(dummy2);

        int i = 0;
        ArrayList<Thread> threads = new ArrayList<>();
        for (ISocketryStress stressClient : stressClients) {
            final int p = i ++;
            final Thread thread = new Thread(() -> stressClient.startFunctionality(server, 100, times, p));
            thread.start();
            threads.add(thread);
        }

        long start = System.nanoTime();
        threads.forEach(arg0 -> {
            try {
                arg0.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.nanoTime();
        double duration = (end - start) / 1_000_000.0;
        System.out.println("Server Took : "  + duration);
        System.out.println("Server Tests passed ...");
        handler.join();
    }
}

