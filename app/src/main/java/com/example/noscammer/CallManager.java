package com.example.noscammer;

import android.telecom.Call;

public class CallManager {

    public static String mainNotificationTitleMessage = "Сервис отклонения неизвестных номеров работает";
    public static String mainNotificationMessage = "Для отключения, измените приложение для звонков по умолчанию";

    private static Call currentCall;

    public static void setCurrentCall(Call call) {
        currentCall = call;
    }

    public static Call getCurrentCall() {
        return currentCall;
    }
}