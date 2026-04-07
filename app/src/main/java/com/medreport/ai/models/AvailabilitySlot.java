package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;

public class AvailabilitySlot {
    @SerializedName("id")           public String id;
    @SerializedName("doctor_id")    public String doctorId;
    @SerializedName("day_of_week")  public String dayOfWeek; // MONDAY, TUESDAY, etc.
    @SerializedName("start_time")   public String startTime; // HH:MM format
    @SerializedName("end_time")     public String endTime;   // HH:MM format
    @SerializedName("is_available") public Boolean isAvailable;
    @SerializedName("slot_duration") public Integer slotDuration; // minutes per appointment
    @SerializedName("created_at")   public String createdAt;
    @SerializedName("updated_at")   public String updatedAt;

    public String getTimeRange() {
        return formatTime(startTime) + " - " + formatTime(endTime);
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

    public String getDayDisplay() {
        if (dayOfWeek == null) return "";
        return dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1).toLowerCase();
    }
}
