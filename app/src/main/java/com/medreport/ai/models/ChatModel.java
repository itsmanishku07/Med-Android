package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatModel {
    @SerializedName("id")              public String id;
    @SerializedName("report_id")       public String reportId;
    @SerializedName("patient_id")      public String patientId;
    @SerializedName("doctor_id")       public String doctorId;
    @SerializedName("created_at")      public String createdAt;
    @SerializedName("last_message_at") public String lastMessageAt;
    @SerializedName("patient_name")    public String patientName;
    @SerializedName("doctor_name")     public String doctorName;
    @SerializedName("report_name")     public String reportName;
    @SerializedName("messages")        public List<MessageModel> messages;
    @SerializedName("unread_count")    public int unreadCount;
}
