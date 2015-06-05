package com.PopCorp.Purchases.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.ListFragment;
import com.PopCorp.Purchases.R;

public class SmsMonitorService extends Service {

    public static final String SMS_BODY = "SMS_BODY";
    public static final String SMS_ADDRESS = "SMS_ADDRESS";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String smsBody = intent.getExtras().getString(SMS_BODY);
        String smsFrom = intent.getExtras().getString(SMS_ADDRESS);
        DB db = new DB(this);
        db.open();
        List newList = List.getListFromSms(db, this, smsBody);
        if (newList!=null){
            Intent intentForStartActivity = new Intent(this, MainActivity.class);
            intentForStartActivity.putExtra(ListFragment.INTENT_TO_LIST_TITLE, newList.getName());
            intentForStartActivity.putExtra(ListFragment.INTENT_TO_LIST_DATELIST, newList.getDatelist());
            createNotify(this, intentForStartActivity, newList.getName(), newList.getDatelist(), smsFrom);
        }
        return START_STICKY;
    }

    private void createNotify(Context context, Intent intentForStartActivity, String title, String datelist, String smsFrom) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intentForStartActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notif = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setContentText(context.getResources().getString(R.string.notify_new_shopping_list_on_sms_from) + " " + smsFrom)
                .setContentTitle(title)
                .setDefaults(Notification.DEFAULT_ALL)
                .setTicker(context.getResources().getString(R.string.notify_new_shopping_list))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pi);

        Notification notification = notif.build();
        notificationManager.notify(Integer.valueOf(datelist.substring(datelist.length()-6)), notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
