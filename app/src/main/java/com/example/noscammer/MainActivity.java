package com.example.noscammer;

import android.Manifest;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.telecom.TelecomManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.noscammer.services.ForegroundNotificationService;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_SET_DEFAULT_DIALER = 123;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Начало");

        // Проверяем, назначено ли наше приложение dialer по умолчанию
        checkAndSetDefaultDialer();
    }

    // Метод для проверки и установки приложения как dialer по умолчанию
    private void checkAndSetDefaultDialer() {
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            String currentDefaultDialer = telecomManager.getDefaultDialerPackage();
            Log.d(TAG, "Текущий dialer по умолчанию: " + currentDefaultDialer);

            if (!currentDefaultDialer.equals(getPackageName())) {
                Log.d(TAG, "Наше приложение не dialer, показываем диалог назначения.");
                offerReplacingDefaultDialer();
            } else {
                Log.d(TAG, "Наше приложение уже dialer по умолчанию.");
                // Если приложение уже назначено как dialer, проверяем разрешения
                checkAndRequestPermissions();
            }
        } else {
            Log.e(TAG, "Не удалось получить TelecomManager.");
        }
    }

    private static final int CHANGE_DEFAULT_DIALER_CODE = 25;

    private void offerReplacingDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            if (roleManager != null) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                startActivityForResult(intent, CHANGE_DEFAULT_DIALER_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, REQUEST_SET_DEFAULT_DIALER);
        }
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

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    // Метод для проверки разрешения на уведомления (для Android 13 и выше)
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (!notificationManager.areNotificationsEnabled()) {
                showGoToSettingsDialog();  // Предлагаем пользователю открыть настройки
                return;
            }
        }

        // Если все разрешения предоставлены, запускаем сервис и закрываем активность
        startServiceAndClose();
    }

    // Метод для обработки результата выбора приложения dialer по умолчанию
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SET_DEFAULT_DIALER) {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null && telecomManager.getDefaultDialerPackage().equals(getPackageName())) {
                // Приложение успешно назначено dialer по умолчанию
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Приложение не назначено dialer по умолчанию", Toast.LENGTH_LONG).show();
                finish();  // Завершаем, если пользователь отказался
            }
        }
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
                showGoToSettingsDialog();  // Если разрешения отклонены
            }
        }
    }

    // Метод для показа диалога с предложением открыть настройки
    private void showGoToSettingsDialog() {
        Toast.makeText(this, "Необходимо предоставить разрешения. Перейдите в настройки приложения.", Toast.LENGTH_LONG).show();
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
    private void startServiceAndClose() {
        Intent serviceIntent = new Intent(this, ForegroundNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Закрываем активность после старта сервиса
        finish();
    }
}