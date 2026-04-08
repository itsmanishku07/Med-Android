package com.medreport.ai.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.medreport.ai.models.WaterReminderSettings;
import java.util.Calendar;

public class WaterReminderScheduler {
    private static final int REQUEST_CODE = 10001;

    public static void schedule(Context ctx, WaterReminderSettings settings) {
        if (!settings.isEnabled()) {
            cancel(ctx);
            return;
        }

        scheduleNext(ctx, settings);
    }

    public static void scheduleNext(Context ctx, WaterReminderSettings settings) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        try {
            Calendar now = Calendar.getInstance();
            Calendar nextAlarm = Calendar.getInstance();
            
            String[] startParts = settings.getStartTime().split(":");
            String[] endParts = settings.getEndTime().split(":");
            int startHour = Integer.parseInt(startParts[0]);
            int startMin = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMin = Integer.parseInt(endParts[1]);

            nextAlarm.add(Calendar.MINUTE, settings.getIntervalMinutes());

            Calendar startTime = (Calendar) now.clone();
            startTime.set(Calendar.HOUR_OF_DAY, startHour);
            startTime.set(Calendar.MINUTE, startMin);
            startTime.set(Calendar.SECOND, 0);

            Calendar endTime = (Calendar) now.clone();
            endTime.set(Calendar.HOUR_OF_DAY, endHour);
            endTime.set(Calendar.MINUTE, endMin);
            endTime.set(Calendar.SECOND, 0);

            if (now.before(startTime)) {
                nextAlarm = startTime;
            }
            else if (nextAlarm.after(endTime)) {
                nextAlarm = (Calendar) startTime.clone();
                nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
            }
            else if (now.after(endTime)) {
                nextAlarm = (Calendar) startTime.clone();
                nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(ctx, WaterReminderReceiver.class);
            intent.putExtra(WaterReminderReceiver.EXTRA_AMOUNT, settings.getWaterAmountMl());

            PendingIntent pi = PendingIntent.getBroadcast(ctx, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.set(AlarmManager.RTC_WAKEUP, nextAlarm.getTimeInMillis(), pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarm.getTimeInMillis(), pi);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, WaterReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
        
        WaterReminderReceiver.stopAlarm(ctx);
    }
}
