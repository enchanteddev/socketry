package com.socketry.StressTests;

import com.socketry.Socketry;

import java.util.concurrent.ExecutionException;

public interface ISocketryStress {
    void startFunctionality(Socketry socketry, int sleepDur, int times, int tunnelId);
}
