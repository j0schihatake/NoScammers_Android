package com.example.noscammer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.example.noscammer.MainActivity;
import com.example.noscammer.R;

public class ForegroundNotificationService extends Service {

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
            stopForeground(true);  // Останавливаем Foreground Service
            stopSelf();  // Останавливаем сам сервис
        }
        return START_STICKY;  // Чтобы сервис перезапускался при необходимости
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundService() {

        createNotificationChannel();

        Intent stopSelfIntent = new Intent(this, ForegroundNotificationService.class);
        stopSelfIntent.setAction(STOP_SERVICE_ACTION);
        PendingIntent stopSelfPendingIntent = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE);

        // Создаем уведомление
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Сервис отклонения неизвестных номеров работает")
                .setContentText("Нажмите 'Отключить' для остановки")
                .setSmallIcon(R.drawable.ic_phone)
                .addAction(R.drawable.ic_stop, "Отключить", stopSelfPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)  // Постоянное уведомление
                .build();

        // Запускаем сервис на переднем плане
        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
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
        stopForeground(true);  // Убедись, что сервис остановлен
    }
}