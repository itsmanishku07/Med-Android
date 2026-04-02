package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UserModel {
    @SerializedName("id")
    public String id;
    @SerializedName("firebase_uid")
    public String firebaseUid;
    @SerializedName("email")
    public String email;
    @SerializedName("name")
    public String name;
    @SerializedName("role")
    public String role;
    @SerializedName("phone")
    public String phone;
    @SerializedName("specializations")
    public List<String> specializations;
    @SerializedName("is_active")
    public boolean isActive;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("profile_picture")
    public String profilePicture;
    @SerializedName("profile")
    public ProfileData profile;

    public static class ProfileData {
        @SerializedName("location")
        public String location;
        @SerializedName("bio")
        public String bio;
    }

    public boolean isPatient() {
        return "PATIENT".equals(role);
    }

    public boolean isDoctor() {
        return "DOCTOR".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public String getInitial() {
        return name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "?";
    }
}
