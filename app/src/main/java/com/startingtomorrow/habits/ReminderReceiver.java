package com.startingtomorrow.habits;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String channel = "habit_reminders";
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26)
            manager.createNotificationChannel(new NotificationChannel(channel, "Напоминания",
                    NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean test = intent != null && intent.getBooleanExtra("test_notification", false);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ?
                new Notification.Builder(context, channel) : new Notification.Builder(context);
        Notification notification = builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(test ? "Проверка уведомлений" : "Starting tomorrow")
                .setContentText(test ? "Отлично! Уведомления приложения работают." : "Самое время" +
                        " уделить внимание своим привычкам")
                .setContentIntent(contentIntent).setAutoCancel(true).build();
        manager.notify(test ? 102 : 101, notification);

        if (!test) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("app_settings"
                    , Context.MODE_PRIVATE);
            if (prefs.getBoolean("reminders", false)) {
                MainActivity.scheduleReminder(context, prefs.getInt("hour", 20), prefs.getInt(
                        "minute", 0));
            }
        }
    }
}
