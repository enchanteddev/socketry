package com.socketry.StressTests;

import com.socketry.Socketry;

public interface ISocketryStress {
    void startFunctionality(Socketry socketry, int sleepDur, int times, int tunnelId);
}
