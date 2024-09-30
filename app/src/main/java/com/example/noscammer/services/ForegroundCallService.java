package com.example.noscammer.services;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.noscammer.CallManager;
import com.example.noscammer.R;

import java.util.HashSet;
import java.util.Set;

public class ForegroundCallService extends InCallService {

    private static final String TAG = "ForegroundCallService";
    private WindowManager windowManager;
    private View callView;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(callCallback);

        // Получаем номер звонка
        String incomingNumber = call.getDetails().getHandle().getSchemeSpecificPart();
        Set<String> contacts = getAllContactNumbers(this);

        // Если номер есть в контактах, не вмешиваемся
        if (contacts.contains(incomingNumber)) {
            Log.d("ForegroundCallService", "Номер найден в контактах: " + incomingNumber);
            return; // Ничего не делаем, стандартная система обработает звонок
        }

        // Если номер не найден в контактах, отклоняем звонок
        CallManager.setCurrentCall(call);
        showCallPanel(incomingNumber);  // Показываем панель звонка
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callCallback);
        removeCallPanel();
        Log.d(TAG, "Звонок удален");
    }

    // Метод для отображения панели звонка
    private void showCallPanel(String incomingNumber) {
        if (windowManager != null && callView != null) {
            return; // Панель уже отображается
        }

        // Настройка WindowManager
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;

        // Создаем и отображаем кастомную панель
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        callView = inflater.inflate(R.layout.activity_call, null);

        // Настройка кнопок принятия и отклонения вызова
        Button acceptButton = callView.findViewById(R.id.acceptButton);
        Button rejectButton = callView.findViewById(R.id.rejectButton);
        TextView callerNumber = callView.findViewById(R.id.callerNumber);
        callerNumber.setText(incomingNumber);

        // Обработка принятия вызова
        acceptButton.setOnClickListener(v -> {
            Call currentCall = CallManager.getCurrentCall();
            if (currentCall != null) {
                currentCall.answer(Call.STATE_ACTIVE);
                removeCallPanel(); // Убираем панель после принятия вызова
            }
        });

        // Обработка отклонения вызова
        rejectButton.setOnClickListener(v -> {
            Call currentCall = CallManager.getCurrentCall();
            if (currentCall != null) {
                currentCall.reject(false, null);
                removeCallPanel(); // Убираем панель после отклонения вызова
            }
        });

        windowManager.addView(callView, params);
    }

    // Метод для удаления панели звонка
    private void removeCallPanel() {
        if (windowManager != null && callView != null) {
            windowManager.removeView(callView);
            callView = null;
        }
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
                contacts.add(phoneNumber.replaceAll("\\s+", ""));  // Убираем пробелы из номеров для корректного сравнения
            }
            cursor.close();
        } else {
            Log.e(TAG, "Не удалось получить список контактов.");
        }

        return contacts;
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            super.onStateChanged(call, state);
            Log.d(TAG, "Состояние звонка изменено: " + state);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Гарантируем, что сервис продолжает работать даже после отклонения звонка
        Log.d(TAG, "ForegroundCallService запущен");
        return START_STICKY;
    }
}