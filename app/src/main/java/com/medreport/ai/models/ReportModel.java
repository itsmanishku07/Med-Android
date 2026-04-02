package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.List;

public class ReportModel {
    @SerializedName("id")                    public String id;
    @SerializedName("patient_id")            public String patientId;
    @SerializedName("assigned_doctor_id")    public String assignedDoctorId;
    @SerializedName("file_name")             public String fileName;
    @SerializedName("file_type")             public String fileType;
    @SerializedName("file_size")             public String fileSize;
    @SerializedName("status")               public String status;
    @SerializedName("medical_specialty")     public String medicalSpecialty;
    @SerializedName("ai_analysis")           public JsonObject aiAnalysis;
    @SerializedName("extracted_text")        public String extractedText;
    @SerializedName("doctor_notes")          public String doctorNotes;
    @SerializedName("uploaded_at")           public String uploadedAt;
    @SerializedName("analyzed_at")           public String analyzedAt;
    @SerializedName("reviewed_at")           public String reviewedAt;
    @SerializedName("assigned_at")           public String assignedAt;
    @SerializedName("is_archived")           public boolean isArchived;
    @SerializedName("doctor_edit_permission") public boolean doctorEditPermission;
    @SerializedName("patient_name")          public String patientName;
    @SerializedName("error_message")         public String errorMessage;
    @SerializedName("suggested_doctors")     public List<SuggestedDoctor> suggestedDoctors;

    public String getSeverityLevel() {
        if (aiAnalysis != null && aiAnalysis.has("severity_level") && !aiAnalysis.get("severity_level").isJsonNull())
            return aiAnalysis.get("severity_level").getAsString();
        return "LOW";
    }

    public String getAiSummary() {
        if (aiAnalysis != null && aiAnalysis.has("summary") && !aiAnalysis.get("summary").isJsonNull())
            return aiAnalysis.get("summary").getAsString();
        return null;
    }

    public String getModelUsed() {
        if (aiAnalysis != null && aiAnalysis.has("model_used") && !aiAnalysis.get("model_used").isJsonNull())
            return aiAnalysis.get("model_used").getAsString();
        return null;
    }

    public boolean isAnalyzed() { return "ANALYZED".equals(status) || "REVIEWED".equals(status); }
    public boolean isPending()  { return "PENDING".equals(status) || "ANALYZING".equals(status); }

    public static class SuggestedDoctor {
        @SerializedName("doctor_id")       public String doctorId;
        @SerializedName("doctor_name")     public String doctorName;
        @SerializedName("match_score")     public double matchScore;
        @SerializedName("match_reason")    public String matchReason;
        @SerializedName("specializations") public List<String> specializations;
    }
}
