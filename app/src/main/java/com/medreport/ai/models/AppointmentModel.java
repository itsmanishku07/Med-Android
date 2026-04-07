package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;

public class AppointmentModel {
    @SerializedName("id")
    public String id;
    
    @SerializedName("patient_id")
    public String patientId;
    
    @SerializedName("doctor_id")
    public String doctorId;
    
    @SerializedName("status")
    public String status; // PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    
    @SerializedName("requested_at")
    public String requestedAt;
    
    @SerializedName("preferred_time")
    public String preferredTime;
    
    @SerializedName("scheduled_at")
    public String scheduledAt;
    
    @SerializedName("notes")
    public String notes;
    
    @SerializedName("doctor_notes")
    public String doctorNotes;
    
    @SerializedName("patient_name")
    public String patientName;
    
    @SerializedName("doctor_name")
    public String doctorName;

    @SerializedName("doctor_email")
    public String doctorEmail;

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isAccepted() {
        return "ACCEPTED".equals(status);
    }
}
