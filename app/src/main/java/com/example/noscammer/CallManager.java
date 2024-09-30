package com.example.noscammer;

import android.telecom.Call;

public class CallManager {
    private static Call currentCall;

    public static void setCurrentCall(Call call) {
        currentCall = call;
    }

    public static Call getCurrentCall() {
        return currentCall;
    }
}