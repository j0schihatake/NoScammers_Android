package com.example.noscammer.call;

import android.telecom.Call;
import io.reactivex.subjects.BehaviorSubject;

/**
 * https://github.com/adnan-creator/java-custom-dialer/tree/master
 */

public class CallManager {

    private static Call currentCall = null;
    private static final BehaviorSubject<Call> subject = BehaviorSubject.create();
    public static final CallManager INSTANCE = new CallManager();

    // Подписка на обновления
    public static BehaviorSubject<Call> updates() {
        return subject;
    }

    // Обновление информации о звонке
    public static void updateCall(Call call) {
        currentCall = call;
        subject.onNext(call);
    }

    // Метод для получения текущего звонка
    public Call getCurrentCall() {
        return currentCall;
    }

    // Метод для отклонения звонка
    public static void rejectCall() {
        if (currentCall != null && currentCall.getState() == Call.STATE_RINGING) {
            currentCall.reject(false, "");
        }
    }

    // Метод для принятия звонка
    public static void acceptCall() {
        if (currentCall != null) {
            currentCall.answer(currentCall.getDetails().getVideoState());
        }
    }

    // Метод для завершения активного звонка
    public static void disconnectCall() {
        if (currentCall != null) {
            currentCall.disconnect();
        }
    }
}

