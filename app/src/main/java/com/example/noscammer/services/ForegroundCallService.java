package com.example.noscammer.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.noscammer.R;

public class ForegroundCallService extends Service {

    private static final String CHANNEL_ID = "call_service_channel";
    private static final String STOP_SERVICE_ACTION = "STOP_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP_SERVICE_ACTION.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Этот сервис не поддерживает привязку, поэтому возвращаем null
        return null;
    }

    private void startForegroundService() {
        createNotificationChannel();

        Intent stopSelfIntent = new Intent(this, ForegroundCallService.class);
        stopSelfIntent.setAction(STOP_SERVICE_ACTION);
        PendingIntent stopSelfPendingIntent = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Сервис работает")
                .setContentText("Нажмите 'Отключить' для остановки")
                .setSmallIcon(R.drawable.ic_phone)
                .addAction(R.drawable.ic_stop, "Отключить", stopSelfPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}