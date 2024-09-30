package com.example.noscammer.services;

import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

public class ForegroundCallService extends InCallService {

    private static final String TAG = "ForegroundCallService";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "Новый звонок добавлен: " + call.getDetails().getHandle().getSchemeSpecificPart());

        // Регистрируем callback для изменений состояния звонка
        call.registerCallback(callCallback);

        // Если звонок входящий, можно обработать его автоматически, например, отклонить его
        if (call.getState() == Call.STATE_RINGING) {
            rejectCall(call);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "Звонок удален");
        call.unregisterCallback(callCallback);
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            super.onStateChanged(call, state);
            Log.d(TAG, "Состояние звонка изменено: " + state);
        }
    };

    // Метод для отклонения звонка
    private void rejectCall(Call call) {
        if (call != null && call.getState() == Call.STATE_RINGING) {
            call.reject(false, null);  // Отклоняем входящий звонок
            Log.d(TAG, "Звонок отклонен.");
        }
    }
}