package com.example.noscammer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.telecom.Call;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity {
    private Call currentCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);  // В вашем layout создайте кнопки для принятия и отклонения вызова

        Button acceptButton = findViewById(R.id.acceptButton);
        Button rejectButton = findViewById(R.id.rejectButton);

        // Получаем текущий звонок
        currentCall = CallManager.getCurrentCall();

        // Обработка принятия звонка
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCall != null) {
                    currentCall.answer(Call.STATE_ACTIVE);
                }
                finish();  // Закрываем активити после принятия
            }
        });

        // Обработка отклонения звонка
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCall != null) {
                    currentCall.reject(false, null);
                }
                finish();  // Закрываем активити после отклонения
            }
        });
    }
}
