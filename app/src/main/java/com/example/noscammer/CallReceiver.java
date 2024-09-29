package com.example.noscammer;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class CallReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID_UNKNOWN_CALL = "unknown_call_channel";
    private static final String CHANNEL_ID_FOREGROUND = "foreground_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && incomingNumber != null) {
            Log.d("CallReceiver", "Входящий звонок от: " + incomingNumber);

            // Проверяем, находится ли номер в контактах
            if (isNumberInContacts(context, incomingNumber)) {
                Log.d("CallReceiver", "Номер в телефонной книге: " + incomingNumber);
                // Номер в контактах, ничего не делаем, чтобы звонок продолжился как обычно
                return;
            }

            // Проверяем, что звонок не является VoIP-звонком (например, из WhatsApp или Telegram)
            if (!isVoIPCall(intent)) {
                Log.d("CallReceiver", "Звонок не VoIP: " + incomingNumber);
                // Отклоняем звонок и уведомляем о неизвестном номере
                endCall(context);
                sendTemporaryNotification(context, incomingNumber);
            }
        }
    }

    // Метод для проверки, находится ли номер в контактах
    private boolean isNumberInContacts(Context context, String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            boolean isContact = cursor.getCount() > 0;
            cursor.close();
            return isContact;
        }
        return false;
    }

    // Метод для проверки, что звонок не является VoIP-звонком
    private boolean isVoIPCall(Intent intent) {
        // Проверяем, не является ли звонок VoIP (например, WhatsApp/Telegram)
        return intent.getBooleanExtra("android.intent.extra.VOIP_CALL", false);
    }

    // Метод для отклонения звонка (звонки из телефонной книги не отклоняются)
    private void endCall(Context context) {
        try {
            // Отклоняем звонок через рефлексию
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> telephonyClass = Class.forName(telephonyManager.getClass().getName());
            Object telephonyInterface = telephonyClass.getDeclaredMethod("getITelephony").invoke(telephonyManager);
            telephonyInterface.getClass().getDeclaredMethod("endCall").invoke(telephonyInterface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Метод для отправки временного уведомления о звонке с неизвестного номера
    private void sendTemporaryNotification(Context context, String number) {
        // Создаем канал уведомлений для неизвестных номеров
        createUnknownCallNotificationChannel(context);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_UNKNOWN_CALL)
                .setContentTitle("Звонок с неизвестного номера")
                .setContentText("Номер: " + number)
                .setSmallIcon(R.drawable.ic_phone)
                .setAutoCancel(true)  // Уведомление автоматически исчезнет при нажатии
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Генерируем уникальный ID для уведомления
        int notificationId = (int) System.currentTimeMillis();

        // Отправляем уведомление
        if (notificationManager != null) {
            notificationManager.notify(notificationId, notification);
        }

        // Удаляем уведомление через 2 минуты (120000 миллисекунд)
        new Handler().postDelayed(() -> {
            if (notificationManager != null) {
                notificationManager.cancel(notificationId);
            }
        }, 120000);
    }

    // Метод для создания канала уведомлений для неизвестных звонков
    private void createUnknownCallNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Неизвестные звонки";
            String description = "Уведомления о звонках с номеров, отсутствующих в телефонной книге";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_UNKNOWN_CALL, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Метод для отправки постоянного уведомления о работе приложения
    public static void sendForegroundNotification(Context context) {
        // Создаем канал уведомлений для работы приложения
        createForegroundNotificationChannel(context);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
                .setContentTitle("Приложение работает")
                .setContentText("Приложение перехватывает звонки")
                .setSmallIcon(R.drawable.ic_phone)
                .setOngoing(true)  // Это постоянное уведомление
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Отправляем постоянное уведомление
        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }
    }

    // Метод для создания канала уведомлений для работы приложения
    private static void createForegroundNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Работа приложения";
            String description = "Уведомление о том, что приложение перехватывает звонки";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_FOREGROUND, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
