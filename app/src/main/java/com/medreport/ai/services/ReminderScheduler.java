package com.medreport.ai.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.medreport.ai.models.ReminderModel;
import java.util.Calendar;
import java.util.List;

public class ReminderScheduler {

    public static void scheduleAll(Context ctx, List<ReminderModel> reminders) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (ReminderModel r : reminders) {
            if (r.isActive) schedule(ctx, am, r);
            else cancel(ctx, r.id);
        }
    }

    public static void schedule(Context ctx, ReminderModel r) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) schedule(ctx, am, r);
    }

    private static void schedule(Context ctx, AlarmManager am, ReminderModel r) {
        try {
            if (r.reminderTime == null || r.reminderTime.length() < 5 || !r.reminderTime.contains(":")) return;
            String[] parts = r.reminderTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min  = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(ctx, AlarmReceiver.class);
            intent.putExtra(AlarmReceiver.EXTRA_NAME,   r.medicineName);
            intent.putExtra(AlarmReceiver.EXTRA_DOSAGE, r.dosage != null ? r.dosage : "");
            intent.putExtra(AlarmReceiver.EXTRA_ID,     r.id);

            int reqCode = r.id != null ? r.id.hashCode() : (int) System.currentTimeMillis();
            PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancel(Context ctx, String reminderId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        int reqCode = reminderId != null ? reminderId.hashCode() : 0;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent,
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) { am.cancel(pi); pi.cancel(); }
    }
}
