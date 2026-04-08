package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class SystemLog {
    @SerializedName("timestamp")
    public String timestamp;
    
    @SerializedName("level")
    public String level; // INFO, WARNING, ERROR
    
    @SerializedName("type")
    public String type; // REQUEST, RESPONSE, ERROR, etc.
    
    @SerializedName("message")
    public String message; // raw log message
    
    @SerializedName("data")
    public Map<String, Object> data; // Additional structured data
    
    // Helper methods to extract common fields from data
    public String getUserId() {
        if (data != null && data.containsKey("user_id")) {
            Object userId = data.get("user_id");
            return userId != null ? userId.toString() : null;
        }
        return null;
    }
    
    public String getUserEmail() {
        if (data != null && data.containsKey("user_email")) {
            Object email = data.get("user_email");
            return email != null ? email.toString() : null;
        }
        return null;
    }
    
    public String getIpAddress() {
        if (data != null && data.containsKey("ip")) {
            Object ip = data.get("ip");
            return ip != null ? ip.toString() : null;
        }
        return null;
    }
    
    public String getMethod() {
        if (data != null && data.containsKey("method")) {
            Object method = data.get("method");
            return method != null ? method.toString() : null;
        }
        return null;
    }
    
    public String getPath() {
        if (data != null && data.containsKey("path")) {
            Object path = data.get("path");
            return path != null ? path.toString() : null;
        }
        return null;
    }
    
    public Integer getStatusCode() {
        if (data != null && data.containsKey("status_code")) {
            Object statusCode = data.get("status_code");
            if (statusCode instanceof Number) {
                return ((Number) statusCode).intValue();
            }
        }
        return null;
    }
}
