package com.example.noscammer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.noscammer.CallUtils;
import com.example.noscammer.R;
import com.example.noscammer.CallManager;

public class ForegroundCallService extends InCallService {

    private static final String TAG = "ForegroundCallService";
    private static final String CHANNEL_ID = "call_service_channel";
    private static final String INCOMING_CALL_CHANNEL_ID = "incoming_call_channel";
    private static final String ACTION_ACCEPT_CALL = "com.example.noscammer.ACCEPT_CALL";
    private static final String ACTION_REJECT_CALL = "com.example.noscammer.REJECT_CALL";
    private static final String ACTION_STOP_SERVICE = "com.example.noscammer.STOP_SERVICE";
    private static final int NOTIFICATION_ID = 1;
    private static final int INCOMING_CALL_NOTIFICATION_ID = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceWithBasicNotification();
    }

    // Основная нотификация, которая будет показываться всегда
    private void startForegroundServiceWithBasicNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(notificationManager);

        Intent stopSelfIntent = new Intent(this, ForegroundCallService.class);
        stopSelfIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopSelfPendingIntent = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Основное уведомление
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Сервис активен")
                .setContentText("Сервис работает для отклонения неизвестных номеров")
                .setSmallIcon(R.drawable.base)
                .addAction(R.drawable.ic_stop, "Отключить", stopSelfPendingIntent) // Кнопка "Отключить"
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        // Запускаем сервис на переднем плане
        startForeground(NOTIFICATION_ID, notification);
    }

    // Метод для создания основного канала уведомлений
    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        call.registerCallback(callCallback);

        String incomingNumber = call.getDetails().getHandle().getSchemeSpecificPart();
        Log.d(TAG, "Номер входящего звонка: " + incomingNumber);

        // Игнорируем VoIP-звонки (например, WhatsApp, Telegram и т.д.)
        if (isVoIPCall(call)) {
            Log.d(TAG, "VoIP-звонок обнаружен, игнорируем его.");
            return;
        }

        // Проверяем, есть ли имя контакта для данного номера
        String contactName = getContactName(incomingNumber, getApplicationContext());

        // Если номер в контактах, показываем уведомление с именем и опциями
        if (contactName != null) {
            Log.d(TAG, "Номер найден в контактах, показываем уведомление.");
            CallManager.setCurrentCall(call);  // Сохраняем текущий звонок
            showIncomingCallNotification(contactName, incomingNumber, true);  // Показываем уведомление с кнопкой "Принять"
        } else {
            // Если номер не в контактах, отклоняем звонок
            Log.d(TAG, "Номер не в контактах, отклоняем звонок.");
            CallUtils.rejectCall(call);
        }
    }

    // Метод для отображения уведомления с кнопками "Принять" и "Отклонить"
    private void showIncomingCallNotification(String contactName, String incomingNumber, boolean showAcceptButton) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createIncomingCallNotificationChannel(notificationManager);

        // Intent для отклонения звонка
        Intent rejectIntent = new Intent(this, ForegroundCallService.class);
        rejectIntent.setAction(ACTION_REJECT_CALL);
        PendingIntent rejectPendingIntent = PendingIntent.getService(this, 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, INCOMING_CALL_CHANNEL_ID)
                .setContentTitle(contactName != null ? contactName : incomingNumber)
                .setContentText("Номер: " + incomingNumber)
                .setSmallIcon(R.drawable.base)
                .addAction(R.drawable.ic_stop, "Отклонить", rejectPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        // Добавляем кнопку "Принять", если нужно
        if (showAcceptButton) {
            Intent acceptIntent = new Intent(this, ForegroundCallService.class);
            acceptIntent.setAction(ACTION_ACCEPT_CALL);
            PendingIntent acceptPendingIntent = PendingIntent.getService(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            notificationBuilder.addAction(R.drawable.ic_phone, "Принять", acceptPendingIntent);
        }

        Notification notification = notificationBuilder.build();
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification);
    }

    // Метод для создания канала уведомлений для входящих звонков
    private void createIncomingCallNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    INCOMING_CALL_CHANNEL_ID,
                    "Входящие звонки",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Call currentCall = CallManager.getCurrentCall();

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_ACCEPT_CALL.equals(action) && currentCall != null) {
                currentCall.answer(Call.STATE_ACTIVE);
                showIncomingCallNotification(null, null, false);  // Убираем кнопку "Принять" из уведомления
            } else if (ACTION_REJECT_CALL.equals(action) && currentCall != null) {
                currentCall.reject(false, null);
                removeNotification(INCOMING_CALL_NOTIFICATION_ID);  // Убираем уведомление после отклонения звонка
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                removeNotification(NOTIFICATION_ID); // Убираем основное уведомление
                stopForeground(true);  // Останавливаем foreground service
                stopSelf();  // Полностью останавливаем сервис
            }
        }

        return START_STICKY;
    }

    // Метод для удаления уведомления
    private void removeNotification(int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(callCallback);
        Log.d(TAG, "Звонок удален");
        removeNotification(INCOMING_CALL_NOTIFICATION_ID);  // Убираем уведомление о звонке
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            Log.d(TAG, "Состояние звонка изменено: " + state);
        }
    };

    // Метод для определения VoIP-звонка
    private boolean isVoIPCall(Call call) {
        Uri handle = call.getDetails().getHandle();
        String scheme = handle.getScheme();
        return scheme != null && (scheme.equalsIgnoreCase("sip") || scheme.contains("whatsapp") || scheme.contains("telegram"));
    }

    // Метод для получения имени контакта по номеру
    private String getContactName(String phoneNumber, Context context) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                cursor.close();
                return contactName;
            }
            cursor.close();
        }
        return null;  // Имя контакта не найдено
    }
}