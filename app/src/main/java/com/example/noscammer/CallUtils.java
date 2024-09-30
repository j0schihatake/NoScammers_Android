package com.example.noscammer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class CallUtils {

    private static final String TAG = "CallUtils";

    // Метод для получения всех номеров из телефонной книги
    public static Set<String> getAllContactNumbers(Context context) {
        Set<String> contacts = new HashSet<>();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.add(phoneNumber.replaceAll("\\s+", ""));  // Убираем пробелы для корректного сравнения
            }
            cursor.close();
        } else {
            Log.e(TAG, "Не удалось получить список контактов.");
        }

        return contacts;
    }

    // Метод для отклонения звонка
    public static void rejectCall(Call call) {
        if (call != null && call.getState() == Call.STATE_RINGING) {
            call.reject(false, null);  // Отклоняем звонок
            Log.d(TAG, "Звонок отклонен.");
        }
    }

    // Метод для проверки, находится ли номер в контактах
    public static boolean isNumberInContacts(String incomingNumber, Context context) {
        Set<String> numbers = getAllContactNumbers(context);
        return numbers.contains(incomingNumber);
    }
}