package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;

public class MessageModel {
    @SerializedName("id")           public String id;
    @SerializedName("chat_id")      public String chatId;
    @SerializedName("sender_id")    public String senderId;
    @SerializedName("sender_role")  public String senderRole;
    @SerializedName("message_type") public String messageType;
    @SerializedName("content")      public String content;
    @SerializedName("image_data")   public String imageData;
    @SerializedName("file_name")    public String fileName;
    @SerializedName("timestamp")    public String timestamp;
    @SerializedName("read")         public boolean read;
    @SerializedName("sender_name")  public String senderName;
}
