package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;

public class BlockedDate {
    @SerializedName("id")          public String id;
    @SerializedName("doctor_id")   public String doctorId;
    @SerializedName("date")        public String date;
    @SerializedName("reason")      public String reason;
    @SerializedName("all_day")     public Boolean allDay;
    @SerializedName("start_time")  public String startTime;
    @SerializedName("end_time")    public String endTime;
    @SerializedName("created_at")  public String createdAt;

    public String getDisplayDate() {
        try {
            String[] parts = date.split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception e) {
            return date;
        }
    }
}
