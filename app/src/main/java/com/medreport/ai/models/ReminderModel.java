package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReminderModel {
    @SerializedName("id")            public String id;
    @SerializedName("medicine_name") public String medicineName;
    @SerializedName("dosage")        public String dosage;
    @SerializedName("reminder_time") public String reminderTime;
    @SerializedName("days")          public List<String> days;
    @SerializedName("is_active")     public boolean isActive;
    @SerializedName("notes")         public String notes;
    @SerializedName("created_at")    public String createdAt;

    public String getDisplayTime() {
        if (reminderTime == null || reminderTime.length() < 5) return reminderTime;
        try {
            String[] parts = reminderTime.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            String ampm = h >= 12 ? "PM" : "AM";
            int h12 = h % 12 == 0 ? 12 : h % 12;
            return String.format("%d:%02d %s", h12, m, ampm);
        } catch (Exception e) {
            return reminderTime;
        }
    }

    public String getDaysDisplay() {
        if (days == null || days.isEmpty()) return "Every day";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            sb.append(days.get(i));
            if (i < days.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}
