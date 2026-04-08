package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("success") public boolean success;
    @SerializedName("message") public String message;
    @SerializedName("user")    public T user;
    @SerializedName("report")  public T report;
    @SerializedName("reminder") public T reminder;
    @SerializedName("count")   public T count;

    public T getData() {
        if (user != null) return user;
        if (report != null) return report;
        if (reminder != null) return reminder;
        return count;
    }
}
