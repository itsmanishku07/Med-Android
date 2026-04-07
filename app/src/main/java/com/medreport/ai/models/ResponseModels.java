package com.medreport.ai.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class ResponseModels {

    public static class ReportsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("reports") public List<ReportModel> reports;
        @SerializedName("count")   public int count;
    }

    public static class NotificationsResponse {
        @SerializedName("success")       public boolean success;
        @SerializedName("notifications") public List<NotificationModel> notifications;
    }

    public static class ChatsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("chats")   public List<ChatModel> chats;
    }

    public static class ChatResponse {
        @SerializedName("success")  public boolean success;
        @SerializedName("chat")     public ChatModel chat;
        @SerializedName("messages") public List<MessageModel> messages;
    }

    public static class RemindersResponse {
        @SerializedName("success")   public boolean success;
        @SerializedName("reminders") public List<ReminderModel> reminders;
    }

    public static class StatsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("stats")   public Map<String, Object> stats;
    }

    public static class UsersResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("users")   public List<UserModel> users;
    }

    public static class AdminDashboardResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("stats")   public Map<String, Object> stats;
        @SerializedName("recent_reports") public List<ReportModel> recentReports;
    }

    public static class AIChatResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("history") public List<AIChatMessage> history;
    }

    public static class AIAskResponse {
        @SerializedName("success")  public boolean success;
        @SerializedName("question") public AIChatMessage question;
        @SerializedName("answer")   public AIChatMessage answer;
    }

    public static class DoctorsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("doctors") public List<UserModel> doctors;
    }

    public static class AppointmentsResponse {
        @SerializedName("success")      public boolean success;
        @SerializedName("appointments") public List<AppointmentModel> appointments;
    }
}
