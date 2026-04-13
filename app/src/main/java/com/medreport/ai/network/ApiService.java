package com.medreport.ai.network;

import com.medreport.ai.models.*;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.Map;

public interface ApiService {

    @POST("auth/register")
    Call<ApiResponse<UserModel>> register(@Body Map<String, String> body);

    @POST("auth/auto-register")
    Call<ApiResponse<UserModel>> autoRegister(@Body Map<String, String> body);

    @POST("auth/signup/request")
    Call<ApiResponse<Map<String, Object>>> signupRequest(@Body Map<String, String> body);

    @POST("auth/signup/verify")
    Call<ApiResponse<UserModel>> signupVerify(@Body Map<String, String> body);

    @GET("auth/profile")
    Call<ApiResponse<UserModel>> getProfile();

    @PUT("auth/profile")
    Call<ApiResponse<UserModel>> updateProfile(@Body Map<String, Object> body);

    @GET("medical-reports/my-reports")
    Call<ResponseModels.ReportsResponse> getMyReports();

    @GET("medical-reports/stats")
    Call<ResponseModels.StatsResponse> getStats();

    @Multipart
    @POST("medical-reports/upload")
    Call<ApiResponse<ReportModel>> uploadReport(
        @Part MultipartBody.Part file,
        @Part("doctor_id") okhttp3.RequestBody doctorId,
        @Part("is_private") okhttp3.RequestBody isPrivate
    );

    @GET("medical-reports/{id}")
    Call<ApiResponse<ReportModel>> getReport(@Path("id") String reportId);

    @DELETE("medical-reports/{id}")
    Call<ApiResponse<Void>> deleteReport(@Path("id") String reportId);

    @POST("medical-reports/{id}/analyze")
    Call<ApiResponse<ReportModel>> analyzeReport(@Path("id") String reportId);

    @GET("medical-reports/private-reports")
    Call<ResponseModels.ReportsResponse> getPrivateReports();

    @POST("medical-reports/{id}/private-assign")
    Call<ApiResponse<ReportModel>> privateAssignDoctor(@Path("id") String reportId, @Body Map<String, String> body);

    @POST("medical-reports/{id}/assign-doctor")
    Call<ApiResponse<ReportModel>> assignDoctor(@Path("id") String reportId, @Body Map<String, String> body);

    @POST("medical-reports/{id}/review")
    Call<ApiResponse<ReportModel>> reviewReport(@Path("id") String reportId, @Body Map<String, String> body);

    @PUT("medical-reports/{id}/archive")
    Call<ApiResponse<ReportModel>> archiveReport(@Path("id") String reportId, @Body Map<String, Boolean> body);

    @PUT("medical-reports/{id}/doctor-edit-permission")
    Call<ApiResponse<ReportModel>> setDoctorEditPermission(@Path("id") String reportId, @Body Map<String, Boolean> body);

    @PUT("medical-reports/{id}/ai-analysis")
    Call<ApiResponse<ReportModel>> updateAiAnalysis(@Path("id") String reportId, @Body Map<String, Object> body);

    @GET("medical-reports/{id}/file")
    Call<okhttp3.ResponseBody> downloadReport(@Path("id") String reportId);

    @GET("notifications/")
    Call<ResponseModels.NotificationsResponse> getNotifications(@Query("unread_only") boolean unreadOnly, @Query("limit") int limit);

    @GET("notifications/unread-count")
    Call<ApiResponse<Integer>> getUnreadCount();

    @PUT("notifications/{id}/read")
    Call<ApiResponse<Void>> markRead(@Path("id") String notifId);

    @PUT("notifications/mark-all-read")
    Call<ApiResponse<Void>> markAllRead();

    @DELETE("notifications/{id}")
    Call<ApiResponse<Void>> deleteNotification(@Path("id") String notifId);

    @GET("chats/")
    Call<ResponseModels.ChatsResponse> getChats();

    @GET("chats/report/{reportId}")
    Call<ResponseModels.ChatResponse> getChatByReport(@Path("reportId") String reportId);

    @POST("chats/{chatId}/messages")
    Call<ApiResponse<MessageModel>> sendMessage(@Path("chatId") String chatId, @Body Map<String, String> body);

    @DELETE("chats/{chatId}")
    Call<ApiResponse<Void>> deleteChat(@Path("chatId") String chatId);

    @GET("medicine-reminders/")
    Call<ResponseModels.RemindersResponse> getReminders();

    @POST("medicine-reminders/")
    Call<ApiResponse<ReminderModel>> createReminder(@Body Map<String, Object> body);

    @PUT("medicine-reminders/{id}")
    Call<ApiResponse<ReminderModel>> updateReminder(@Path("id") String id, @Body Map<String, Object> body);

    @DELETE("medicine-reminders/{id}")
    Call<ApiResponse<Void>> deleteReminder(@Path("id") String id);

    @GET("admin/dashboard")
    Call<ResponseModels.AdminDashboardResponse> getAdminDashboard();

    @GET("admin/users")
    Call<ResponseModels.UsersResponse> getAllUsers();

    @GET("logs/list")
    Call<ResponseModels.SystemLogsResponse> getSystemLogs(@Query("page") int page, @Query("per_page") int perPage);

    @GET("logs/statistics")
    Call<ResponseModels.LogStatisticsResponse> getLogStatistics(@Query("hours") int hours);

    @GET("logs/settings")
    Call<ResponseModels.LogSettingsResponse> getLogSettings();

    @PUT("logs/settings")
    Call<ResponseModels.LogSettingsResponse> updateLogSettings(@Body Map<String, Boolean> settings);

    @DELETE("logs/clear")
    Call<ApiResponse<Void>> clearLogs();

    @GET("logs/download")
    Call<okhttp3.ResponseBody> downloadLogs();

    @PUT("admin/users/{uid}/role")
    Call<ApiResponse<UserModel>> updateUserRole(@Path("uid") String uid, @Body Map<String, String> body);

    @PUT("admin/users/{uid}/status")
    Call<ApiResponse<UserModel>> updateUserStatus(@Path("uid") String uid, @Body Map<String, Boolean> body);

    @GET("auth/doctors")
    Call<ResponseModels.DoctorsResponse> getDoctors();

    @POST("appointments/request")
    Call<ApiResponse<AppointmentModel>> requestAppointment(@Body Map<String, Object> body);

    @GET("appointments/patient")
    Call<ResponseModels.AppointmentsResponse> getPatientAppointments();

    @GET("appointments/doctor")
    Call<ResponseModels.AppointmentsResponse> getDoctorAppointments();

    @PUT("appointments/{id}/status")
    Call<ApiResponse<AppointmentModel>> updateAppointmentStatus(@Path("id") String id, @Body Map<String, Object> body);

    @GET("medical-reports/{id}/ai-chat")
    Call<ResponseModels.AIChatResponse> getAIChatHistory(@Path("id") String reportId);

    @DELETE("medical-reports/{id}/ai-chat")
    Call<ApiResponse<Void>> deleteAIChatHistory(@Path("id") String reportId);

    @POST("medical-reports/{id}/ask")
    Call<ResponseModels.AIAskResponse> askAIQuestion(@Path("id") String reportId, @Body Map<String, String> body);

    @GET("availability/slots")
    Call<ResponseModels.AvailabilitySlotsResponse> getMyAvailabilitySlots();

    @POST("availability/slots")
    Call<ApiResponse<AvailabilitySlot>> createAvailabilitySlot(@Body Map<String, Object> body);

    @PUT("availability/slots/{id}")
    Call<ApiResponse<AvailabilitySlot>> updateAvailabilitySlot(@Path("id") String slotId, @Body Map<String, Object> body);

    @DELETE("availability/slots/{id}")
    Call<ApiResponse<Void>> deleteAvailabilitySlot(@Path("id") String slotId);

    @GET("availability/blocked-dates")
    Call<ResponseModels.BlockedDatesResponse> getMyBlockedDates();

    @POST("availability/block-date")
    Call<ApiResponse<BlockedDate>> createBlockedDate(@Body Map<String, Object> body);

    @DELETE("availability/blocked-dates/{id}")
    Call<ApiResponse<Void>> deleteBlockedDate(@Path("id") String blockedDateId);

    @GET("availability/calendar-slots")
    Call<ResponseModels.CalendarSlotsResponse> getCalendarSlots(@Query("start_date") String startDate, @Query("end_date") String endDate);

    @POST("availability/calendar-slots")
    Call<ApiResponse<AvailabilitySlot>> createCalendarSlot(@Body Map<String, Object> body);

    @DELETE("availability/calendar-slots/{id}")
    Call<ApiResponse<Void>> deleteCalendarSlot(@Path("id") String slotId);

    @POST("availability/apply-template")
    Call<ResponseModels.ApplyTemplateResponse> applyWeeklyTemplate(@Body Map<String, Object> body);

    @GET("availability/doctor/{doctorId}")
    Call<ResponseModels.DoctorAvailabilityResponse> getDoctorAvailability(@Path("doctorId") String doctorId);

    @GET("reviews/doctor/{doctorId}")
    Call<ResponseModels.DoctorReviewsResponse> getDoctorReviews(@Path("doctorId") String doctorId);

    @GET("reviews/doctor/{doctorId}/my-review")
    Call<ResponseModels.MyReviewResponse> getMyReview(@Path("doctorId") String doctorId);

    @POST("reviews/doctor/{doctorId}")
    Call<ResponseModels.SubmitReviewResponse> submitReview(@Path("doctorId") String doctorId, @Body Map<String, Object> body);

    @DELETE("reviews/{reviewId}")
    Call<ApiResponse<Void>> deleteReview(@Path("reviewId") String reviewId);

    @GET("reviews/all-stats")
    Call<ResponseModels.AllReviewStatsResponse> getAllReviewStats();

    // Database Admin endpoints
    @GET("database-admin/tables")
    Call<ResponseModels.DatabaseTablesResponse> getDatabaseTables();

    @GET("database-admin/tables/{tableName}")
    Call<ResponseModels.TableDataResponse> getTableData(@Path("tableName") String tableName, @Query("page") int page, @Query("per_page") int perPage);

    @DELETE("database-admin/tables/{tableName}/record/{recordId}")
    Call<ApiResponse<Void>> deleteRecord(@Path("tableName") String tableName, @Path("recordId") String recordId);

    @DELETE("database-admin/tables/{tableName}/clear")
    Call<ResponseModels.ClearTableResponse> clearTable(@Path("tableName") String tableName, @Query("confirm") String confirm);

    @DELETE("database-admin/users/{userId}")
    Call<ResponseModels.DeleteUserResponse> deleteUserCompletely(@Path("userId") String userId);
}
