package com.socketry;

import java.util.HashMap;
import java.util.function.Function;

public class Socketry {
    byte[] socketsPerChannel;
    HashMap<String, Function<byte[], byte[]>> procedures;
    String[] procedureNames;


    byte[] getProcedures() {
        return null;
    }

    public Socketry(byte[] socketsPerChannel,
            HashMap<String, Function<byte[], byte[]>> procedures) {
        this.socketsPerChannel = socketsPerChannel;
        this.procedures = procedures;
        this.procedures.put("getProcedures", i -> this.getProcedures());

        this.procedureNames = new String[procedures.size()];
        this.procedureNames[0] = "getProcedures";

        int index = 1;
        for (String key : procedures.keySet()) {
            if (!key.equals("getProcedures")) {
                this.procedureNames[index++] = key;
            }
        }
    }

    public byte[] handleRemoteCall(byte fnId, byte[] data) {
        Function<byte[], byte[]> procedure = procedures.get(procedureNames[fnId]);
        if (procedure == null) {
            throw new IllegalArgumentException("Unknown procedure: " + procedureNames[fnId]);
        }

        return procedure.apply(data);
    }
}