package com.example.noscammer;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.noscammer.services.ForegroundCallService;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверяем все необходимые разрешения
        checkAndRequestPermissions();
    }

    // Метод для проверки всех необходимых разрешений
    private void checkAndRequestPermissions() {
        if (checkPermissions()) {
            // Если все разрешения предоставлены, проверяем разрешение на уведомления
            checkNotificationPermission();
        } else {
            // Запрашиваем разрешения
            requestPermissions();
        }
    }

    // Метод для проверки всех стандартных разрешений
    private boolean checkPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
        };

        // Если хотя бы одно из разрешений не предоставлено, возвращаем false
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;  // Все стандартные разрешения предоставлены
    }

    // Метод для запроса стандартных разрешений
    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
        };

        boolean shouldShowRationale = false;
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                shouldShowRationale = true;
                break;
            }
        }

        if (shouldShowRationale) {
            Toast.makeText(this, "Приложению нужны разрешения для корректной работы.", Toast.LENGTH_LONG).show();
        }

        // Запрашиваем недостающие разрешения
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    // Метод для проверки разрешения на уведомления (для Android 13 и выше)
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {  // Используем числовое значение для Android 13 и выше
            // Проверяем, доступны ли уведомления
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (!notificationManager.areNotificationsEnabled()) {
                // Проверяем, есть ли разрешение на отправку уведомлений
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // Если все разрешения предоставлены, запускаем сервис и закрываем активность
        startServiceAndFinish();
    }

    // Обработка результата запроса разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermissions()) {
                // Проверяем разрешение на уведомления
                checkNotificationPermission();
            } else {
                // Если разрешения не предоставлены
                boolean shouldShowRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (!shouldShowRationale) {
                    // Если выбрано "не спрашивать снова"
                    showGoToSettingsDialog();
                } else {
                    Toast.makeText(this, "Разрешения отклонены.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            // Обработка результата запроса разрешений для уведомлений
            if (Build.VERSION.SDK_INT >= 33) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (notificationManager != null && notificationManager.areNotificationsEnabled()) {
                    // Если разрешение на уведомления предоставлено, запускаем сервис
                    startServiceAndFinish();
                } else {
                    Toast.makeText(this, "Разрешение на уведомления отклонено.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Метод для показа диалога с предложением открыть настройки
    private void showGoToSettingsDialog() {
        Toast.makeText(this, "Перейдите в настройки, чтобы предоставить разрешения.", Toast.LENGTH_LONG).show();
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }, 2000); // Задержка перед переходом в настройки
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Проверяем разрешения при возврате из настроек
        if (checkPermissions()) {
            // Проверяем разрешение на уведомления
            checkNotificationPermission();
        }
    }

    // Метод для запуска сервиса и закрытия активности
    private void startServiceAndFinish() {
        Intent serviceIntent = new Intent(this, ForegroundCallService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Закрываем активность после старта сервиса
        finish();
    }
}