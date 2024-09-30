package com.example.noscammer;

import android.content.Context;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import com.example.noscammer.call.CallManager;

import java.util.HashSet;
import java.util.Set;

public class CallReceiver extends InCallService {

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(callCallback);
        Log.d("CallReceiver", "Добавлен новый звонок: " + call.getDetails().getHandle().getSchemeSpecificPart());

        // Получаем все номера из телефонной книги
        Set<String> contacts = getAllContactNumbers(this);
        String incomingNumber = call.getDetails().getHandle().getSchemeSpecificPart();

        // Проверяем, есть ли входящий номер в контактах
        if (contacts.contains(incomingNumber)) {
            Log.d("CallReceiver", "Номер в телефонной книге: " + incomingNumber);
            return;  // Номер найден, ничего не делаем
        }

        // Отклоняем звонок, если номер не в контактах
        CallManager.updateCall(call);
        rejectCall(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callCallback);
        CallManager.updateCall(null);
    }

    private Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            CallManager.updateCall(call);
        }
    };

    // Метод для отклонения звонка
    private void rejectCall(Call call) {
        if (call != null && call.getState() == Call.STATE_RINGING) {
            call.reject(false, null);  // Отклоняем звонок
            Log.d("CallReceiver", "Звонок отклонен.");
        }
    }

    // Метод для получения всех номеров из телефонной книги
    private Set<String> getAllContactNumbers(Context context) {
        Set<String> contacts = new HashSet<>();
        // Здесь можно реализовать логику для получения всех контактов из телефонной книги
        // Для этого можно использовать ContentResolver для получения данных из контактов пользователя
        return contacts;
    }
}
