package com.startingtomorrow.habits;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.AlarmManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("app_settings",
                Context.MODE_PRIVATE);
        if (!prefs.getBoolean("reminders", false))
            return;
        if (Build.VERSION.SDK_INT >= 31 && !((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms())
            return;
        MainActivity.scheduleReminder(context, prefs.getInt("hour", 20), prefs.getInt("minute", 0));
    }
}
