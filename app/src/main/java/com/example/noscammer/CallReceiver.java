package com.example.noscammer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && incomingNumber != null) {
            if (!isNumberInContacts(context, incomingNumber)) {
                // Отклонение звонка
                endCall(context);
                // Уведомление о странном номере (можно доработать для вывода в UI или лог)
                sendNotification(context, incomingNumber);
            }
        }
    }

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

    private void endCall(Context context) {
        try {
            // Отклонение звонка через рефлексию
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> telephonyClass = Class.forName(telephonyManager.getClass().getName());
            Object telephonyInterface = telephonyClass.getDeclaredMethod("getITelephony").invoke(telephonyManager);
            telephonyInterface.getClass().getDeclaredMethod("endCall").invoke(telephonyInterface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(Context context, String number) {
        // Здесь можно добавить логику уведомления о звонке с неизвестного номера
    }
}
