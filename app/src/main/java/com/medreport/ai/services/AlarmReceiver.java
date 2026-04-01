package com.medreport.ai.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import com.medreport.ai.MedReportApp;
import com.medreport.ai.R;
import com.medreport.ai.activities.MedicineRemindersActivity;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP  = "com.medreport.ai.STOP_ALARM";
    public static final String EXTRA_NAME   = "medicine_name";
    public static final String EXTRA_DOSAGE = "dosage";
    public static final String EXTRA_ID     = "reminder_id";
    private static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ACTION_STOP.equals(intent.getAction())) {
            stopAlarm(ctx);
            return;
        }

        String name   = intent.getStringExtra(EXTRA_NAME);
        String dosage = intent.getStringExtra(EXTRA_DOSAGE);
        String rid    = intent.getStringExtra(EXTRA_ID);

        playAlarm(ctx);
        showNotification(ctx, name, dosage, rid);
    }

    @SuppressWarnings("deprecation")
    private void playAlarm(Context ctx) {
        try {
            stopAlarm(ctx); // stop any previous
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            mediaPlayer.setDataSource(ctx, alarmUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) { e.printStackTrace(); }

        // Vibrate
        Vibrator v = getVibrator(ctx);
        if (v != null) {
            long[] pattern = {0, 500, 300, 500, 300, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                v.vibrate(pattern, 0);
            }
        }
    }

    public static void stopAlarm(Context ctx) {
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        Vibrator v = getVibrator(ctx);
        if (v != null) v.cancel();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(999);
    }

    @SuppressWarnings("deprecation")
    private static Vibrator getVibrator(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.os.VibratorManager vm = (android.os.VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        } else {
            return (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void showNotification(Context ctx, String name, String dosage, String rid) {
        // Stop action
        Intent stopIntent = new Intent(ctx, AlarmReceiver.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getBroadcast(ctx, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Open app action (Main Activity)
        Intent openIntent = new Intent(ctx, com.medreport.ai.activities.MainActivity.class);
        openIntent.putExtra("target_tab", "reminders");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String body = "Time to take " + name + (dosage != null && !dosage.isEmpty() ? " — " + dosage : "");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, MedReportApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("💊 Medicine Reminder")
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(openPi, true)
            .addAction(R.drawable.ic_stop, "Stop Alarm", stopPi)
            .setContentIntent(openPi);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(999, builder.build());
    }
}
