package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;

public class AIChatMessage {
    @SerializedName("id")
    public String id;
    
    @SerializedName("report_id")
    public String reportId;
    
    @SerializedName("role")
    public String role;
    
    @SerializedName("content")
    public String content;
    
    @SerializedName("timestamp")
    public String timestamp;

    public AIChatMessage() {}

    public AIChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
    }
}
