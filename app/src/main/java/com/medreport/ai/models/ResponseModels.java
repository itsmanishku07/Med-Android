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

    public static class SystemLogsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("logs")    public List<SystemLog> logs;
        @SerializedName("pagination") public LogPagination pagination;
        @SerializedName("logging_enabled") public boolean loggingEnabled;
    }
    
    public static class LogPagination {
        @SerializedName("total") public int total;
        @SerializedName("page") public int page;
        @SerializedName("per_page") public int perPage;
        @SerializedName("total_pages") public int totalPages;
    }

    public static class LogStatisticsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("statistics") public LogStatistics statistics;
        @SerializedName("hours") public int hours;
        @SerializedName("logging_enabled") public boolean loggingEnabled;
    }

    public static class LogStatistics {
        @SerializedName("total_requests") public int totalRequests;
        @SerializedName("total_errors") public int totalErrors;
        @SerializedName("total_warnings") public int totalWarnings;
        @SerializedName("avg_response_time") public double avgResponseTime;
        @SerializedName("requests_by_hour") public Map<String, Integer> requestsByHour;
        @SerializedName("requests_by_endpoint") public Map<String, Integer> requestsByEndpoint;
        @SerializedName("status_codes") public Map<String, Integer> statusCodes;
        @SerializedName("recent_errors") public List<RecentError> recentErrors;
    }

    public static class RecentError {
        @SerializedName("timestamp") public String timestamp;
        @SerializedName("error_type") public String errorType;
        @SerializedName("error") public String error;
        @SerializedName("path") public String path;
    }

    public static class LogSettingsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("settings") public LogSettings settings;
        @SerializedName("message") public String message;
    }

    public static class LogSettings {
        @SerializedName("enabled") public boolean enabled;
        @SerializedName("console_enabled") public boolean consoleEnabled;
        @SerializedName("log_level") public String logLevel;
        @SerializedName("log_file_path") public String logFilePath;
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

    public static class AvailabilitySlotsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("slots")   public List<AvailabilitySlot> slots;
    }

    public static class BlockedDatesResponse {
        @SerializedName("success")       public boolean success;
        @SerializedName("blocked_dates") public List<BlockedDate> blockedDates;
    }

    public static class DoctorAvailabilityResponse {
        @SerializedName("success")       public boolean success;
        @SerializedName("slots")         public List<AvailabilitySlot> slots;
        @SerializedName("blocked_dates") public List<BlockedDate> blockedDates;
    }

    public static class DoctorReviewsResponse extends ApiResponse<Void> {
        public List<DoctorReview> reviews;
        public ReviewStats stats;
    }

    public static class MyReviewResponse extends ApiResponse<Void> {
        public DoctorReview review;
    }

    public static class SubmitReviewResponse extends ApiResponse<Void> {
        public DoctorReview review;
        public ReviewStats stats;
    }

    public static class AllReviewStatsResponse extends ApiResponse<Void> {
        public Map<String, ReviewStats> stats;
    }

    public static class CalendarSlotsResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("slots") public List<AvailabilitySlot> slots;
        @SerializedName("weekly_template") public List<AvailabilitySlot> weeklyTemplate;
        @SerializedName("blocked_dates") public List<BlockedDate> blockedDates;
    }

    public static class ApplyTemplateResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("message") public String message;
        @SerializedName("slots_created") public int slotsCreated;
    }

    // Database Admin Models
    public static class DatabaseTable {
        @SerializedName("name") public String name;
        @SerializedName("row_count") public int rowCount;
    }

    public static class DatabaseTablesResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("tables") public List<DatabaseTable> tables;
    }

    public static class TableData {
        @SerializedName("columns") public List<String> columns;
        @SerializedName("rows") public List<Map<String, Object>> rows;
        @SerializedName("total") public int total;
        @SerializedName("page") public int page;
        @SerializedName("per_page") public int perPage;
        @SerializedName("total_pages") public int totalPages;
    }

    public static class TableDataResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("data") public TableData data;
    }

    public static class ClearTableResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("message") public String message;
        @SerializedName("deleted_count") public int deletedCount;
    }

    public static class DeleteUserResponse {
        @SerializedName("success") public boolean success;
        @SerializedName("message") public String message;
        @SerializedName("deleted_from_db") public boolean deletedFromDb;
        @SerializedName("deleted_from_firebase") public boolean deletedFromFirebase;
    }
}
