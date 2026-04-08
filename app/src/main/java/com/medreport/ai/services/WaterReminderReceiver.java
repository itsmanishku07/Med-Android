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
import com.medreport.ai.models.WaterReminderSettings;

public class WaterReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP = "com.medreport.ai.STOP_WATER_ALARM";
    public static final String ACTION_DISMISS = "com.medreport.ai.DISMISS_WATER_ALARM";
    public static final String EXTRA_AMOUNT = "water_amount";
    
    private static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        
        if (ACTION_STOP.equals(action) || ACTION_DISMISS.equals(action)) {
            stopAlarm(ctx);
            return;
        }

        WaterReminderSettings settings = WaterReminderSettings.load(ctx);
        if (!settings.isEnabled()) {
            return;
        }

        int amount = intent.getIntExtra(EXTRA_AMOUNT, 250);
        
        playAlarm(ctx);
        showNotification(ctx, amount);
        
        WaterReminderScheduler.scheduleNext(ctx, settings);
    }

    @SuppressWarnings("deprecation")
    private void playAlarm(Context ctx) {
        try {
            stopAlarm(ctx);
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            mediaPlayer.setDataSource(ctx, alarmUri);
            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Vibrator v = getVibrator(ctx);
        if (v != null) {
            long[] pattern = {0, 300, 200, 300};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        }
    }

    public static void stopAlarm(Context ctx) {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        Vibrator v = getVibrator(ctx);
        if (v != null) v.cancel();
        
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(1001);
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

    private void showNotification(Context ctx, int amount) {
        Intent dismissIntent = new Intent(ctx, WaterReminderReceiver.class);
        dismissIntent.setAction(ACTION_DISMISS);
        PendingIntent dismissPi = PendingIntent.getBroadcast(ctx, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(ctx, com.medreport.ai.activities.MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String amountText = amount >= 1000 ? (amount / 1000.0) + "L" : amount + "ml";
        String body = "Time to drink " + amountText + " of water 💧";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, MedReportApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("💧 Water Reminder")
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Done", dismissPi)
            .setContentIntent(openPi);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1001, builder.build());
    }
}
