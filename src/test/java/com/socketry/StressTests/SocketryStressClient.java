package com.socketry.StressTests;

import com.socketry.Socketry;
import com.socketry.SocketryClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;


class StressClientClass1 implements ISocketryStress {


    int add(int a, int b) {
        return a + b;
    }

    byte[] time(byte[] args) {
        long curr = System.nanoTime();
        return ByteBuffer.allocate(8).putLong(curr).array();
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

    void assertHello(Socketry socketry, int tunnelId) throws InterruptedException, ExecutionException {
        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.SERVER_FUNC_HELLO);
        long startTime = System.nanoTime();
        byte[] result = socketry.makeRemoteCall(fnId, new byte[0], tunnelId).get();
        long endTime = System.nanoTime();
        long duration = endTime - startTime;

//        System.out.println("Execution time Hello: " + duration / 1_000_000 + " ms");
        assertEquals("Hello, World!", new String(result));
    }

    long readLong(byte[] bytes) {
        // get the first 8 bytes without extra allocation
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getLong();
    }

    double checkTime(Socketry socketry, int tunnelId, int i) throws InterruptedException, ExecutionException {
        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.SERVER_FUNC_TIME);
        long start = System.nanoTime();
        byte[] endbytes = socketry.makeRemoteCall(fnId, new byte[0], tunnelId).get();
        long finish = System.nanoTime();
        long end = readLong(endbytes);
        long duration = (end - start) / 1_000_000;
        double duration2 = (finish - start) / 1_000_000.0;
        return duration2;
//        System.out.println("Client " + i +  " --- Received time : " + duration + " ms " + "Round Trip time : " + duration2 + " ms = " + "Diff: " + (duration2 - duration)  + " ms");
    }

    @Override
    public void startFunctionality(Socketry socketry, int sleepDur, int times, int tunnelId) {
        System.out.println("Starting Hello " + tunnelId);
        int i = 0;
        double total = 0;
        while (times -- > 0) {
            try {
//                System.out.println("Waiting for hello");
                // assertHello(socketry, tunnelId);
//                System.out.println("Client Class1 Ran: " + i ++);
                total += checkTime(socketry, tunnelId, i ++);
                Thread.sleep(sleepDur);
            } catch (Exception e) {
                System.out.println("Error : " + e.getMessage());
                e.printStackTrace();
                assert (false);
            }
        }
        System.out.println("Client Class1 Ran: " + total + "Average time: " + (total / i)); 
    }
}

/**
 * Simulate complex Functions
 */
class StressClientUI implements ISocketryStress {

    public StressClientUI() {
        bigData = new byte[1024 * 1024 * 10];
        for (int i = 0; i < bigData.length; i++) {
            bigData[i] = (byte) (Math.random() * 255);
        }
    }

    byte[] bigData;

    byte[] updateScreenUI(short[][][] image) throws InterruptedException {
        Thread.sleep(1000);
        return new byte[0];
    }
     
    byte[] updateScreenUIWrapper(byte[] args) {
        ByteBuffer buffer = ByteBuffer.wrap(args);
        short[][][] image = new short[3][3][3];
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                for (int k = 0; k < image[0][0].length; k++) {
                    image[i][j][k] = buffer.getShort();
                }
            }
        }
        byte[] result;
        try {
            result = updateScreenUI(image);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new byte[0];
        }
        return result;
    }

    ////////////
    /// CLient Functions for validation
    ////////////
    
    short[][][] addFilterValidator(short[][][] a, short[][][] b) {
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

    long addMultipliedValidator(ArrayList<Integer> numbers) {
        long result = 0;
        for (int i = 0; i < numbers.size(); i++) {
            result += numbers.get(i);
        }
        return result;
    }
    

    ////////////
    /// Serielazers for server functions
    ////////////
   
    byte[] serealizeImage(short[][][] image) {
        ByteBuffer buffer = ByteBuffer.allocate(image.length * image[0].length * image[0][0].length * 2 + 12);
        buffer.putInt(image.length);
        buffer.putInt(image[0].length); 
        buffer.putInt(image[0][0].length);
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                for (int k = 0; k < image[0][0].length; k++) {
                    buffer.putShort(image[i][j][k]);
                }
            }
        }
        return buffer.array();
    }

    short[][][] deserealizeImage(byte[] image) {
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

    byte[] addFilterSerialise(short[][][] a, short[][][] b) {
        byte[] image1 = serealizeImage(a);
        byte[] image2 = serealizeImage(b);
        return ByteBuffer.allocate(image1.length + image2.length).put(image1).put(image2).array();
    }

    void checkBigDataExchange(Socketry socketry, int tunnelId, byte[] data) throws InterruptedException, ExecutionException {
        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.SERVER_BIGDATAEXCHANGE);
        byte[] result = socketry.makeRemoteCall(fnId, data, tunnelId).get();
        assertEquals(data.length, result.length);
    }

    
    void callNAssertAddMultiplied(Socketry socketry, int tunnelId, int times)
        throws InterruptedException, ExecutionException {
        // create a random numbers
        ArrayList<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            numbers.add((int) (Math.random() * 100));
        }

        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.SERVER_ADDMULTIPLIED);
        ByteBuffer allocate = ByteBuffer.allocate(numbers.size() * 8);
        numbers.forEach(allocate::putInt);
        byte[] args = allocate.array();
        byte[] result = socketry.makeRemoteCall(fnId, args, tunnelId).get();
        long result2 = addMultipliedValidator(numbers);
        assertEquals(result2, ByteBuffer.wrap(result).getLong());
    }

    void callNAssertAddFilter(Socketry socketry, int tunnelId, int times) throws InterruptedException, ExecutionException {
        byte fnId = socketry.getRemoteProcedureId(SocketryStressFunc.SERVER_ADDFILTER);

        short[][][] a = new short[1920][1200][3];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                for (int k = 0; k < a[0][0].length; k++) {
                    a[i][j][k] = (short) (Math.random() * 100);
                }
            }
        }

        short[][][] b = new short[1920][1200][3];
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                for (int k = 0; k < b[0][0].length; k++) {
                    b[i][j][k] = (short) (Math.random() * 100);
                }
            }
        }

        byte[] args = addFilterSerialise(a, b);
        byte[] result = socketry.makeRemoteCall(fnId, args, tunnelId).get();
        short[][][] result2 = deserealizeImage(result);
        short[][][] resext = addFilterValidator(a, b);
        assertArray(resext, result2);
    }

    

    void assertArray(short[][][] a, short[][][] b) {
        // assert length
        assertEquals(a.length, b.length);
        assertEquals(a[0].length, b[0].length);
        assertEquals(a[0][0].length, b[0][0].length);

        // assert values
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                for (int k = 0; k < a[0][0].length; k++) {
                    assertEquals(a[i][j][k], b[i][j][k]);
                }
            }
        }
    }

    @Override
    public void startFunctionality(Socketry socketry, int sleepDur, int times, int tunnelId) {
        System.out.println("Starting Screen UI : " + tunnelId);
        // int i = 0;
        while (times -- > 0) {
            try {
                // call AddMultiplied
                 callNAssertAddMultiplied(socketry, tunnelId, times);

                 callNAssertAddFilter(socketry, tunnelId, times);

//                checkBigDataExchange(socketry, tunnelId, bigData);
//                System.out.println("Client UI Ran : " + i ++);
                Thread.sleep(sleepDur);
            } catch (Exception e) {
                System.out.println("Error : " + e.getMessage());
                assert (false);
            }
        }
    }
}

public class SocketryStressClient {

    public static void start(int times) throws Exception {
        HashMap<String, Function<byte[], byte[]>> procedures = new HashMap<>();

        StressClientClass1 dummy = new StressClientClass1();
        procedures.put(SocketryStressFunc.CLIENT_FUNC_ADD, dummy::addWrapper);
        procedures.put(SocketryStressFunc.CLIENT_FUNC_TIME, dummy::time);

        StressClientUI dummy2 = new StressClientUI();
        procedures.put(SocketryStressFunc.CLIENT_FUNC_UPDATE_SCREEN_UI, dummy2::updateScreenUIWrapper);

        SocketryClient client = new SocketryClient(new byte[] {5, 5, 5}, 60000, procedures);
        System.out.println("Client started");

        Thread handler = new Thread(client::listenLoop);
        handler.start();

        // start multiple threads
        ArrayList<ISocketryStress> stressClients = new ArrayList<>();

        stressClients.add(dummy);
//        stressClients.add(dummy2);

        int i = 0;
        ArrayList<Thread> threads = new ArrayList<>();
        for (ISocketryStress stressClient : stressClients) {
            final int p = i ++;
            final Thread thread = new Thread(() -> stressClient.startFunctionality(client, 100, times, p));
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
        System.out.println("Client Took : "  + duration);

        System.out.println("Client Tests passed ...");
        handler.interrupt();
    }
}

