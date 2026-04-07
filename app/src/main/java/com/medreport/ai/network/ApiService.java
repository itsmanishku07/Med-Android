package com.medreport.ai.network;

import com.medreport.ai.models.*;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.Map;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
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

    // ── Reports ───────────────────────────────────────────────────────────────
    @GET("medical-reports/my-reports")
    Call<ResponseModels.ReportsResponse> getMyReports();

    @GET("medical-reports/stats")
    Call<ResponseModels.StatsResponse> getStats();

    @Multipart
    @POST("medical-reports/upload")
    Call<ApiResponse<ReportModel>> uploadReport(
        @Part MultipartBody.Part file,
        @Part("report_type") okhttp3.RequestBody reportType
    );

    @GET("medical-reports/{id}")
    Call<ApiResponse<ReportModel>> getReport(@Path("id") String reportId);

    @DELETE("medical-reports/{id}")
    Call<ApiResponse<Void>> deleteReport(@Path("id") String reportId);

    @POST("medical-reports/{id}/analyze")
    Call<ApiResponse<ReportModel>> analyzeReport(@Path("id") String reportId);

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

    // ── Notifications ─────────────────────────────────────────────────────────
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

    // ── Chat ──────────────────────────────────────────────────────────────────
    @GET("chats/")
    Call<ResponseModels.ChatsResponse> getChats();

    @GET("chats/report/{reportId}")
    Call<ResponseModels.ChatResponse> getChatByReport(@Path("reportId") String reportId);

    @POST("chats/{chatId}/messages")
    Call<ApiResponse<MessageModel>> sendMessage(@Path("chatId") String chatId, @Body Map<String, String> body);

    @DELETE("chats/{chatId}")
    Call<ApiResponse<Void>> deleteChat(@Path("chatId") String chatId);

    // ── Medicine Reminders ────────────────────────────────────────────────────
    @GET("medicine-reminders/")
    Call<ResponseModels.RemindersResponse> getReminders();

    @POST("medicine-reminders/")
    Call<ApiResponse<ReminderModel>> createReminder(@Body Map<String, Object> body);

    @PUT("medicine-reminders/{id}")
    Call<ApiResponse<ReminderModel>> updateReminder(@Path("id") String id, @Body Map<String, Object> body);

    @DELETE("medicine-reminders/{id}")
    Call<ApiResponse<Void>> deleteReminder(@Path("id") String id);

    // ── Admin ─────────────────────────────────────────────────────────────────
    @GET("admin/dashboard")
    Call<ResponseModels.AdminDashboardResponse> getAdminDashboard();

    @GET("admin/users")
    Call<ResponseModels.UsersResponse> getAllUsers();

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

    // ── AI Q&A ────────────────────────────────────────────────────────────────
    @GET("medical-reports/{id}/ai-chat")
    Call<ResponseModels.AIChatResponse> getAIChatHistory(@Path("id") String reportId);

    @DELETE("medical-reports/{id}/ai-chat")
    Call<ApiResponse<Void>> deleteAIChatHistory(@Path("id") String reportId);

    @POST("medical-reports/{id}/ask")
    Call<ResponseModels.AIAskResponse> askAIQuestion(@Path("id") String reportId, @Body Map<String, String> body);
}
