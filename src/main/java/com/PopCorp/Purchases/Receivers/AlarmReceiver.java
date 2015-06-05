package com.PopCorp.Purchases.Receivers;

import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Fragments.ListFragment;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
	
	public static final String ALARM_INTENT_TITLE = "title";
	public static final String ALARM_INTENT_DATELIST = "datelist";
	
	@Override//������� ����������� � ����������� ������
	public void onReceive(Context context, Intent intent) {
		Bundle extra = intent.getExtras();
		String title = extra.getString(ALARM_INTENT_TITLE);
		String datelist = extra.getString(ALARM_INTENT_DATELIST);
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WACELOCK Purchases");
		wakeLock.acquire();
		Intent intentForStartActivity = new Intent(context, MainActivity.class);
		intentForStartActivity.putExtra(ListFragment.INTENT_TO_LIST_TITLE, title);
		intentForStartActivity.putExtra(ListFragment.INTENT_TO_LIST_DATELIST, datelist);

		createNotify(context, intentForStartActivity, title, datelist);
		wakeLock.release();
	}

	private void createNotify(Context context, Intent intentForStartActivity, String title, String datelist) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		PendingIntent pi = PendingIntent.getActivity(context, 0, intentForStartActivity, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder notif = new NotificationCompat.Builder(context)
		.setSmallIcon(R.drawable.ic_notification_alarm)
		.setAutoCancel(true)
		.setContentText(context.getResources().getString(R.string.notify_time_to_shopping))
		.setContentTitle(title)
		.setDefaults(Notification.DEFAULT_ALL)
		.setTicker(context.getResources().getString(R.string.notify_time_to_shopping))
		.setWhen(System.currentTimeMillis())
		.setContentIntent(pi);
		
		Notification notification = notif.build();
		notificationManager.notify(Integer.valueOf(datelist.substring(datelist.length()-6)), notification);
	}

	public void cancelAlarm(Context context, String name, String datelist) {
		Intent intent = new Intent(context, AlarmReceiver.class);//������ �����������
		intent.setAction(name + datelist);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}

	public void setAlarm(Context context, long dateInLong, String name, String datelist) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setAction(name + datelist);
		Bundle args = new Bundle();
		args.putString(ALARM_INTENT_TITLE, name.toString());
		args.putString(ALARM_INTENT_DATELIST, datelist.toString());
		intent.putExtras(args);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.RTC_WAKEUP, dateInLong, pendingIntent);
	}
}
