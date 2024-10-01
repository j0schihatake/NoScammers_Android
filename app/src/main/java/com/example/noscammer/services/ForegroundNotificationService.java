package com.example.noscammer.services;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.noscammer.CallManager;
import com.example.noscammer.R;

public class ForegroundNotificationService extends Service {

    private static final String CHANNEL_ID = "call_service_channel";
    private static final String STOP_SERVICE_ACTION = "STOP_SERVICE";
    private static final int CHECK_DEFAULT_DIALER_INTERVAL = 5000;
    private Handler handler = new Handler();
    private Runnable checkDialerRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        startDialerCheck();
    }

    /**
     * Метод для старта таймера проверки смены dialer-а
     */
    private void startDialerCheck() {
        checkDialerRunnable = new Runnable() {
            @Override
            public void run() {
                TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
                if (telecomManager != null) {
                    String currentDefaultDialer = telecomManager.getDefaultDialerPackage();
                    if (!currentDefaultDialer.equals(getPackageName())) {
                        stopForegroundServices();
                    } else {
                        handler.postDelayed(this, CHECK_DEFAULT_DIALER_INTERVAL);
                    }
                }
            }
        };

        handler.post(checkDialerRunnable);
    }

    /**
     * Остановка сервисов
     */
    private void stopForegroundServices() {
        // Удаляем уведомление и останавливаем сервис
        stopForeground(true);
        stopSelf();

        // Удаляем уведомление
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        Log.d(TAG, "Dialer по умолчанию изменен, сервисы остановлены.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP_SERVICE_ACTION.equals(intent.getAction())) {
            stopForegroundServices();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkDialerRunnable);  // Очищаем handler, чтобы избежать утечек памяти
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundService() {
        createNotificationChannel();

        // Создаем уведомление
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(CallManager.mainNotificationTitleMessage)
                .setContentText(CallManager.mainNotificationMessage)
                .setSmallIcon(R.drawable.base)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
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
}