package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.util.List;

public class ReportModel {
    @SerializedName("id")                    public String id;
    @SerializedName("patient_id")            public String patientId;
    @SerializedName("assigned_doctor_id")    public String assignedDoctorId;
    @SerializedName("file_name")             public String fileName;
    @SerializedName("file_type")             public String fileType;
    @SerializedName("file_size")             public String fileSize;
    @SerializedName("status")                public String status;
    @SerializedName("medical_specialty")     public String medicalSpecialty;
    @SerializedName("ai_analysis")           public JsonElement aiAnalysis;
    @SerializedName("extracted_text")        public String extractedText;
    @SerializedName("doctor_notes")          public String doctorNotes;
    @SerializedName("uploaded_at")           public String uploadedAt;
    @SerializedName("analyzed_at")           public String analyzedAt;
    @SerializedName("reviewed_at")           public String reviewedAt;
    @SerializedName("assigned_at")           public String assignedAt;
    @SerializedName("is_archived")           public Boolean isArchived;
    @SerializedName("doctor_edit_permission") public Boolean doctorEditPermission;
    @SerializedName("patient_name")          public String patientName;
    @SerializedName("error_message")         public String errorMessage;
    @SerializedName("suggested_doctors")     public List<SuggestedDoctor> suggestedDoctors;
    @SerializedName("file_url")              public String fileUrl; // URL to view/download original file

    public String getSeverityLevel() {
        if (aiAnalysis != null && aiAnalysis.isJsonObject()) {
            JsonObject obj = aiAnalysis.getAsJsonObject();
            if (obj.has("severity_level") && !obj.get("severity_level").isJsonNull())
                return obj.get("severity_level").getAsString();
        }
        return "LOW";
    }

    public String getAiSummary() {
        if (aiAnalysis != null && aiAnalysis.isJsonObject()) {
            JsonObject obj = aiAnalysis.getAsJsonObject();
            if (obj.has("summary") && !obj.get("summary").isJsonNull())
                return obj.get("summary").getAsString();
        }
        return null;
    }

    public String getModelUsed() {
        if (aiAnalysis != null && aiAnalysis.isJsonObject()) {
            JsonObject obj = aiAnalysis.getAsJsonObject();
            if (obj.has("model_used") && !obj.get("model_used").isJsonNull())
                return obj.get("model_used").getAsString();
        }
        return null;
    }

    public boolean isAnalyzed() { return "ANALYZED".equals(status) || "REVIEWED".equals(status); }
    public boolean isPending()  { return "PENDING".equals(status) || "ANALYZING".equals(status); }

    public static class SuggestedDoctor {
        @SerializedName("doctor_id")       public String doctorId;
        @SerializedName("doctor_name")     public String doctorName;
        @SerializedName("match_score")     public Double matchScore;
        @SerializedName("match_reason")    public String matchReason;
        @SerializedName("specializations") public List<String> specializations;
    }
}
