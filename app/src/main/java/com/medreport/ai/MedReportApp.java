package com.medreport.ai;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MedReportApp extends Application {
    public static final String CHANNEL_REMINDERS = "medicine_reminders";
    public static final String CHANNEL_GENERAL    = "general";

    @Override
    public void onCreate() {
        super.onCreate();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS, "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH);
            reminders.setDescription("Alerts when it's time to take medicine");
            reminders.enableVibration(true);
            reminders.setVibrationPattern(new long[]{0, 500, 200, 500});
            nm.createNotificationChannel(reminders);

            NotificationChannel general = new NotificationChannel(
                CHANNEL_GENERAL, "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT);
            general.setDescription("Report updates, messages, and system alerts");
            nm.createNotificationChannel(general);
        }
    }
}
