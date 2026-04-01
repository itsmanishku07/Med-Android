package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;

public class NotificationModel {
    @SerializedName("id")                public String id;
    @SerializedName("notification_type") public String type;
    @SerializedName("title")             public String title;
    @SerializedName("message")           public String message;
    @SerializedName("related_id")        public String relatedId;
    @SerializedName("read")              public boolean read;
    @SerializedName("created_at")        public String createdAt;
}
