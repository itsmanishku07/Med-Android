package com.medreport.ai.models;

public class DoctorReview {
    public String id;
    public int rating;
    public String comment;
    public String created_at;
    public String updated_at;
    public PatientInfo patient;

    public static class PatientInfo {
        public String id;
        public String name;
        public String profile_picture;
    }
}
