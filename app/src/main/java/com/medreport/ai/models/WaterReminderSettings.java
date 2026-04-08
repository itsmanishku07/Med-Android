package com.medreport.ai.models;

import android.content.Context;
import android.content.SharedPreferences;

public class WaterReminderSettings {
    private static final String PREFS_NAME = "water_reminder_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_INTERVAL = "interval_minutes";
    private static final String KEY_AMOUNT = "water_amount_ml";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";

    private boolean enabled;
    private int intervalMinutes;
    private int waterAmountMl;
    private String startTime;
    private String endTime;

    public WaterReminderSettings() {
        this.enabled = false;
        this.intervalMinutes = 60;
        this.waterAmountMl = 250;
        this.startTime = "08:00";
        this.endTime = "22:00";
    }

    public static WaterReminderSettings load(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        WaterReminderSettings settings = new WaterReminderSettings();
        settings.enabled = prefs.getBoolean(KEY_ENABLED, false);
        settings.intervalMinutes = prefs.getInt(KEY_INTERVAL, 60);
        settings.waterAmountMl = prefs.getInt(KEY_AMOUNT, 250);
        settings.startTime = prefs.getString(KEY_START_TIME, "08:00");
        settings.endTime = prefs.getString(KEY_END_TIME, "22:00");
        return settings;
    }

    public void save(Context ctx) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_ENABLED, enabled);
        editor.putInt(KEY_INTERVAL, intervalMinutes);
        editor.putInt(KEY_AMOUNT, waterAmountMl);
        editor.putString(KEY_START_TIME, startTime);
        editor.putString(KEY_END_TIME, endTime);
        editor.apply();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }

    public int getWaterAmountMl() { return waterAmountMl; }
    public void setWaterAmountMl(int waterAmountMl) { this.waterAmountMl = waterAmountMl; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getIntervalDisplay() {
        if (intervalMinutes < 60) {
            return intervalMinutes + " minutes";
        } else {
            int hours = intervalMinutes / 60;
            int mins = intervalMinutes % 60;
            if (mins == 0) {
                return hours + (hours == 1 ? " hour" : " hours");
            } else {
                return hours + "h " + mins + "m";
            }
        }
    }

    public String getAmountDisplay() {
        if (waterAmountMl >= 1000) {
            double liters = waterAmountMl / 1000.0;
            return String.format("%.1fL", liters);
        } else {
            return waterAmountMl + "ml";
        }
    }
}
