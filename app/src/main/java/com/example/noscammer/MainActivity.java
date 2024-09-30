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

import com.example.noscammer.services.ForegroundCallService;
import com.example.noscammer.services.ForegroundNotificationService;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_SET_DEFAULT_DIALER = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверяем, назначено ли наше приложение dialer по умолчанию
        checkAndSetDefaultDialer();
    }

    // Метод для проверки и установки приложения как dialer по умолчанию
    private void checkAndSetDefaultDialer() {
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            String currentDefaultDialer = telecomManager.getDefaultDialerPackage();
            Log.d("MainActivity", "Текущий dialer по умолчанию: " + currentDefaultDialer);

            if (!currentDefaultDialer.equals(getPackageName())) {
                Log.d("MainActivity", "Наше приложение не dialer, показываем диалог назначения.");
                offerReplacingDefaultDialer();
            } else {
                Log.d("MainActivity", "Наше приложение уже dialer по умолчанию.");
                checkAndRequestPermissions();
            }
        } else {
            Log.e("MainActivity", "Не удалось получить TelecomManager.");
        }
    }

    private static final int CHANGE_DEFAULT_DIALER_CODE = 25;

    private void offerReplacingDefaultDialer() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    this.getPackageName());
            startActivity(intent);
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
            startActivityForResult(intent, CHANGE_DEFAULT_DIALER_CODE);
        }
    }

    // Метод для проверки всех необходимых разрешений
    private void checkAndRequestPermissions() {
        if (checkPermissions()) {
            // Если все разрешения предоставлены, запускаем сервисы
            startServices();
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
                startServices();  // Запуск сервисов после получения разрешений
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

    // Метод для запуска сервисов (уведомления и обработки звонков)
    private void startServices() {
        // Запускаем сервис уведомлений
        Intent notificationServiceIntent = new Intent(this, ForegroundNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notificationServiceIntent);
        } else {
            startService(notificationServiceIntent);
        }

        // Запускаем InCallService для управления звонками
        Intent callServiceIntent = new Intent(this, ForegroundCallService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(callServiceIntent);
        } else {
            startService(callServiceIntent);
        }

        // Закрываем активность после запуска сервисов
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            startServices();
        }
    }
}