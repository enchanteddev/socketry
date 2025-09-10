package com.socketry;

import java.util.function.Function;

import com.socketry.json.IJsonable;


public class Socketry {
    byte[] socketsPerChannel;
    Function<IJsonable, IJsonable>[] procedures;
    
    public Socketry() {
        
    }
}