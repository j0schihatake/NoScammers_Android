package com.example.noscammer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.noscammer.CallUtils;
import com.example.noscammer.R;
import com.example.noscammer.CallManager;

public class ForegroundCallService extends InCallService {

    private static final String TAG = "ForegroundCallService";
    private static final String CHANNEL_ID = "incoming_call_channel";
    private static final String ACTION_ACCEPT_CALL = "com.example.noscammer.ACCEPT_CALL";
    private static final String ACTION_REJECT_CALL = "com.example.noscammer.REJECT_CALL";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(callCallback);

        String incomingNumber = call.getDetails().getHandle().getSchemeSpecificPart();
        Log.d(TAG, "Номер входящего звонка: " + incomingNumber);

        // Если номер в контактах, показываем уведомление с опциями
        if (CallUtils.isNumberInContacts(incomingNumber, getApplicationContext())) {
            Log.d(TAG, "Номер найден в контактах, показываем уведомление.");
            CallManager.setCurrentCall(call);  // Сохраняем текущий звонок
            showIncomingCallNotification(incomingNumber);  // Показываем уведомление
        } else {
            // Если номер не в контактах, отклоняем звонок
            Log.d(TAG, "Номер не в контактах, отклоняем звонок.");
            CallUtils.rejectCall(call);
        }
    }

    private void showIncomingCallNotification(String incomingNumber) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(notificationManager);

        Intent acceptIntent = new Intent(this, ForegroundCallService.class);
        acceptIntent.setAction(ACTION_ACCEPT_CALL);
        PendingIntent acceptPendingIntent = PendingIntent.getService(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent rejectIntent = new Intent(this, ForegroundCallService.class);
        rejectIntent.setAction(ACTION_REJECT_CALL);
        PendingIntent rejectPendingIntent = PendingIntent.getService(this, 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Входящий звонок")
                .setContentText("Номер: " + incomingNumber)
                .setSmallIcon(R.drawable.base)
                .addAction(R.drawable.ic_phone, "Принять", acceptPendingIntent)
                .addAction(R.drawable.ic_stop, "Отклонить", rejectPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)  // Уведомление будет оставаться до завершения звонка
                .build();

        notificationManager.notify(1, notification);
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Входящие звонки",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Call currentCall = CallManager.getCurrentCall();

        if (intent != null && currentCall != null) {
            String action = intent.getAction();
            if (ACTION_ACCEPT_CALL.equals(action)) {
                currentCall.answer(Call.STATE_ACTIVE);
                removeNotification();
            } else if (ACTION_REJECT_CALL.equals(action)) {
                currentCall.reject(false, null);
                removeNotification();
            }
        }

        return START_STICKY;
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callCallback);
        Log.d(TAG, "Звонок удален");
        removeNotification();
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            Log.d(TAG, "Состояние звонка изменено: " + state);
        }
    };
}