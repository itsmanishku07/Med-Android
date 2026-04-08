package com.medreport.ai.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.medreport.ai.databinding.ActivityWaterReminderSettingsBinding;
import com.medreport.ai.models.WaterReminderSettings;
import com.medreport.ai.services.WaterReminderScheduler;

public class WaterReminderSettingsActivity extends AppCompatActivity {
    private ActivityWaterReminderSettingsBinding b;
    private WaterReminderSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityWaterReminderSettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        settings = WaterReminderSettings.load(this);
        setupUI();
        setupListeners();
    }

    private void setupUI() {
        b.switchEnable.setChecked(settings.isEnabled());
        updateSettingsVisibility();
        updateIntervalDisplay();
        updateAmountDisplay();
        updateTimeDisplay();
        updateDailyGoal();
    }

    private void setupListeners() {
        b.btnBack.setOnClickListener(v -> finish());

        b.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setEnabled(isChecked);
            updateSettingsVisibility();
        });

        b.btnIntervalMinus.setOnClickListener(v -> {
            int current = settings.getIntervalMinutes();
            if (current > 15) {
                settings.setIntervalMinutes(current - 15);
                updateIntervalDisplay();
                updateDailyGoal();
            }
        });

        b.btnIntervalPlus.setOnClickListener(v -> {
            int current = settings.getIntervalMinutes();
            if (current < 480) {
                settings.setIntervalMinutes(current + 15);
                updateIntervalDisplay();
                updateDailyGoal();
            }
        });

        b.btnAmountMinus.setOnClickListener(v -> {
            int current = settings.getWaterAmountMl();
            if (current > 100) {
                settings.setWaterAmountMl(current - 50);
                updateAmountDisplay();
                updateDailyGoal();
            }
        });

        b.btnAmountPlus.setOnClickListener(v -> {
            int current = settings.getWaterAmountMl();
            if (current < 1000) {
                settings.setWaterAmountMl(current + 50);
                updateAmountDisplay();
                updateDailyGoal();
            }
        });

        b.layoutStartTime.setOnClickListener(v -> showTimePicker(true));
        b.layoutEndTime.setOnClickListener(v -> showTimePicker(false));

        b.btnSave.setOnClickListener(v -> saveSettings());
    }

    private void updateSettingsVisibility() {
        int visibility = settings.isEnabled() ? View.VISIBLE : View.GONE;
        b.layoutSettings.setVisibility(visibility);
    }

    private void updateIntervalDisplay() {
        b.tvInterval.setText(settings.getIntervalDisplay());
    }

    private void updateAmountDisplay() {
        b.tvAmount.setText(settings.getAmountDisplay());
    }

    private void updateTimeDisplay() {
        b.tvStartTime.setText(formatTime(settings.getStartTime()));
        b.tvEndTime.setText(formatTime(settings.getEndTime()));
    }

    private void updateDailyGoal() {
        try {
            // Calculate active hours
            String[] startParts = settings.getStartTime().split(":");
            String[] endParts = settings.getEndTime().split(":");
            int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
            
            int activeMinutes = endMinutes - startMinutes;
            if (activeMinutes < 0) activeMinutes += 24 * 60; // Handle overnight
            
            // Calculate number of reminders
            int reminders = activeMinutes / settings.getIntervalMinutes();
            
            // Calculate total water
            int totalMl = reminders * settings.getWaterAmountMl();
            
            // Format display
            String goalText;
            if (totalMl >= 1000) {
                float liters = totalMl / 1000f;
                goalText = String.format("You'll drink ~%.1fL per day (%d reminders)", liters, reminders);
            } else {
                goalText = String.format("You'll drink ~%dml per day (%d reminders)", totalMl, reminders);
            }
            
            b.tvDailyGoal.setText(goalText);
        } catch (Exception e) {
            b.tvDailyGoal.setText("Configure your settings above");
        }
    }

    private String formatTime(String time24) {
        try {
            String[] parts = time24.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            String ampm = h >= 12 ? "PM" : "AM";
            int h12 = h % 12 == 0 ? 12 : h % 12;
            return String.format("%d:%02d %s", h12, m, ampm);
        } catch (Exception e) {
            return time24;
        }
    }

    private void showTimePicker(boolean isStartTime) {
        String currentTime = isStartTime ? settings.getStartTime() : settings.getEndTime();
        String[] parts = currentTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        TimePickerDialog picker = new TimePickerDialog(this,
            (view, hourOfDay, min) -> {
                String time24 = String.format("%02d:%02d", hourOfDay, min);
                if (isStartTime) {
                    settings.setStartTime(time24);
                } else {
                    settings.setEndTime(time24);
                }
                updateTimeDisplay();
                updateDailyGoal();
            }, hour, minute, false);
        
        picker.setTitle(isStartTime ? "Select Start Time" : "Select End Time");
        picker.show();
    }

    private void saveSettings() {
        settings.save(this);
        
        if (settings.isEnabled()) {
            WaterReminderScheduler.schedule(this, settings);
            Toast.makeText(this, "Water reminders enabled!", Toast.LENGTH_SHORT).show();
        } else {
            WaterReminderScheduler.cancel(this);
            Toast.makeText(this, "Water reminders disabled", Toast.LENGTH_SHORT).show();
        }
        
        finish();
    }
}
