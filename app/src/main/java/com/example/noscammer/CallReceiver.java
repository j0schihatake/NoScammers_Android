package com.example.noscammer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class CallReceiver extends InCallService {

    private static final String TAG = "CallReceiver";

    private String message = null;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(callCallback);

        // Получаем номер звонка
        String incomingNumber = call.getDetails().getHandle().getSchemeSpecificPart();
        Set<String> contacts = getAllContactNumbers(this);

        // Если номер есть в контактах, показываем интерфейс
        if (contacts.contains(incomingNumber)) {
            Log.d("CallReceiver", "Номер найден в контактах: " + incomingNumber);
            CallManager.setCurrentCall(call);  // Сохраняем текущий звонок
            showCallActivity();  // Показываем интерфейс для управления звонком
            return;
        }

        // Если номер не найден в контактах, отклоняем звонок
        rejectCall(call);
    }

    private void showCallActivity() {
        Intent intent = new Intent(this, CallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callCallback);
        message("Звонок удален.");
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            super.onStateChanged(call, state);
            message("Изменение состояния звонка: " + state);
        }
    };

    // Метод для отклонения звонка
    private void rejectCall(Call call) {
        if (call != null && call.getState() == Call.STATE_RINGING) {
            call.reject(false, null);  // Отклоняем звонок
            message("Звонок отклонен.");
        } else {
            message("Звонок не может быть отклонен, так как не находится в состоянии звонка.");

        }
    }

    private void message(String message){
        Log.d(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Метод для получения всех номеров из телефонной книги
    private Set<String> getAllContactNumbers(Context context) {
        Set<String> contacts = new HashSet<>();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.add(phoneNumber.replaceAll("\\s+", ""));  // Убираем пробелы из номеров
            }
            cursor.close();
        } else {
            message("Не удалось получить список контактов.");
        }

        return contacts;  // Возвращаем множество номеров
    }
}
