package com.example.noscammer.services;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class ForegroundCallService extends InCallService {

    private static final String TAG = "ForegroundCallService";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(callCallback);

        // Получаем номер звонка
        String incomingNumber = call.getDetails().getHandle().getSchemeSpecificPart();
        Set<String> contacts = getAllContactNumbers(this);

        // Если номер есть в контактах, не вмешиваемся
        if (contacts.contains(incomingNumber)) {
            Log.d("CallReceiver", "Номер найден в контактах: " + incomingNumber);
            return;  // Система Android продолжит обрабатывать звонок
        }

        // Если номер не найден в контактах, отклоняем звонок
        rejectCall(call);
    }

    private Set<String> getAllContactNumbers(Context context) {
        Set<String> contacts = new HashSet<>();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.add(phoneNumber.replaceAll("\\s+", ""));  // Убираем пробелы из номеров для корректного сравнения
            }
            cursor.close();
        } else {
            Log.e("CallReceiver", "Не удалось получить список контактов.");
        }

        return contacts;  // Возвращаем множество номеров
    }


    private void rejectCall(Call call) {
        if (call != null && call.getState() == Call.STATE_RINGING) {
            call.reject(false, null);  // Отклоняем звонок
            Log.d("CallReceiver", "Звонок отклонен.");
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Гарантируем, что сервис продолжает работать даже после отклонения звонка
        Log.d(TAG, "ForegroundCallService запущен");
        return START_STICKY;
    }
}