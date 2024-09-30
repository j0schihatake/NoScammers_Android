package com.example.noscammer;

import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

public class CallReceiver extends InCallService {

    private static final String TAG = "CallReceiver";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(callCallback);

        String incomingNumber = call.getDetails().getHandle().getSchemeSpecificPart();

        if (CallUtils.isNumberInContacts(incomingNumber, getApplicationContext())) {
            Log.d(TAG, "Звонок от известного номера.");
        } else {
            CallUtils.rejectCall(call);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callCallback);
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            super.onStateChanged(call, state);
            if (state == Call.STATE_DISCONNECTED) {
                Log.d(TAG, "Звонок завершен");
            }
        }
    };
}