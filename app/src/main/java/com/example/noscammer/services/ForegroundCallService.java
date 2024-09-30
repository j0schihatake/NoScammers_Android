package com.example.noscammer.services;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

public class ForegroundCallService extends InCallService {

    private static final String TAG = "ForegroundCallService";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "Новый звонок добавлен: " + call.getDetails().getHandle().getSchemeSpecificPart());

        try {
            // Регистрируем callback для изменений состояния звонка
            call.registerCallback(callCallback);

            // Если звонок входящий и номер неизвестен, отклоняем его
            if (call.getState() == Call.STATE_RINGING) {
                Log.d(TAG, "Входящий звонок, проверка отклонения...");
                rejectCall(call);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при добавлении звонка: " + e.getMessage());
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        try {
            Log.d(TAG, "Звонок удален");
            call.unregisterCallback(callCallback);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при удалении звонка: " + e.getMessage());
        }
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            super.onStateChanged(call, state);
            Log.d(TAG, "Состояние звонка изменено: " + state);

            // Дополнительная проверка состояния звонка на случай изменений
            if (state == Call.STATE_DISCONNECTED) {
                Log.d(TAG, "Звонок завершен");
            }
        }
    };

    // Метод для отклонения звонка
    private void rejectCall(Call call) {
        try {
            if (call != null && call.getState() == Call.STATE_RINGING) {
                Log.d(TAG, "Отклоняем звонок.");
                call.reject(false, null);  // Отклоняем входящий звонок
            } else {
                Log.d(TAG, "Звонок не в состоянии звонка, пропускаем.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отклонении звонка: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Гарантируем, что сервис продолжает работать даже после отклонения звонка
        Log.d(TAG, "ForegroundCallService запущен");
        return START_STICKY;
    }
}