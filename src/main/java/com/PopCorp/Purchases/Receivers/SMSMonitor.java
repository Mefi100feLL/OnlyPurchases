package com.PopCorp.Purchases.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

import com.PopCorp.Purchases.Services.SmsMonitorService;

public class SMSMonitor extends BroadcastReceiver {

    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null && intent.getAction() != null && ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
            Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
            SmsMessage[] messages = new SmsMessage[pduArray.length];
            for (int i = 0; i < pduArray.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
            }

            String sms_from = messages[0].getDisplayOriginatingAddress();
            StringBuilder bodyText = new StringBuilder();
            for (SmsMessage message : messages) {
                bodyText.append(message.getMessageBody());
            }
            String body = bodyText.toString();
            Intent mIntent = new Intent(context, SmsMonitorService.class);
            mIntent.putExtra(SmsMonitorService.SMS_BODY, body);
            mIntent.putExtra(SmsMonitorService.SMS_ADDRESS, sms_from);
            context.startService(mIntent);
        }
    }
}
