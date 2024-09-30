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
import com.example.noscammer.R;

public class ForegroundNotificationService extends Service {

    private static final String CHANNEL_ID = "notification_service_channel";
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
        return START_STICKY;
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

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Приложение перехватывает звонки")
                .setContentText("Нажмите 'Отключить', чтобы остановить сервис")
                .setSmallIcon(R.drawable.ic_phone)
                .addAction(R.drawable.ic_stop, "Отключить", stopSelfPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)  // Постоянное уведомление
                .build();

        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notification Service Channel",
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
        stopForeground(true);  // Останавливаем уведомление
    }
}
