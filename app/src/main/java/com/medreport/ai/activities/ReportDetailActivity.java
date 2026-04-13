package com.medreport.ai.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.medreport.ai.R;
import com.medreport.ai.databinding.ActivityReportDetailBinding;
import com.medreport.ai.fragments.ReportActionsBottomSheet;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.utils.AuthManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDetailActivity extends AppCompatActivity {
    public static final String EXTRA_REPORT_ID = "report_id";
    private static final String TAG = "ReportDetail";
    private ActivityReportDetailBinding b;
    private ReportModel report;
    private String reportId;
    private io.noties.markwon.Markwon markwon;
    private boolean canEdit = false;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean isPolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        b = ActivityReportDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        markwon = io.noties.markwon.Markwon.create(this);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        b.swipeRefresh.setOnRefreshListener(() -> {
            stopPolling();
            loadReport();
        });
        loadReport();
    }


    private void loadReport() {
        b.swipeRefresh.setRefreshing(true);
        ApiClient.get().getReport(reportId).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                b.swipeRefresh.setRefreshing(false);
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    Log.d(TAG, "Report loaded. Status=" + report.status + " aiAnalysis="
                            + (report.aiAnalysis != null ? "present" : "null"));
                    bindReport();
                    if ("ANALYZING".equals(report.status))
                        startPolling();
                    else
                        stopPolling();
                } else {
                    Log.e(TAG, "Load failed. Code=" + r.code());
                    Toast.makeText(ReportDetailActivity.this, "Failed to load report", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                b.swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Network error", t);
                Toast.makeText(ReportDetailActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void startPolling() {
        if (isPolling)
            return;
        isPolling = true;
        // Legacy b.btnAnalyze is now a dummy View to avoid breaking data binding
        pollRunnable = () -> ApiClient.get().getReport(reportId).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    bindReport();
                    if ("ANALYZING".equals(report.status)) {
                        if (isPolling)
                            pollHandler.postDelayed(pollRunnable, 5000);
                    } else {
                        stopPolling();
                        Toast.makeText(ReportDetailActivity.this, "Analysis complete!", Toast.LENGTH_SHORT).show();
                        loadReport();
                    }
                } else if (isPolling)
                    pollHandler.postDelayed(pollRunnable, 5000);
            }

            @Override
            public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                if (isPolling)
                    pollHandler.postDelayed(pollRunnable, 5000);
            }
        });
        pollHandler.postDelayed(pollRunnable, 5000);
    }

    private void stopPolling() {
        isPolling = false;
        if (pollRunnable != null)
            pollHandler.removeCallbacks(pollRunnable);
        b.btnAnalyzePrimary.setEnabled(true);
        b.btnAnalyzePrimary.setText("Start AI Analysis");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }


    private void bindReport() {
        if (report == null)
            return;

        UserModel me = AuthManager.getInstance().getCurrentUser();
        boolean isPatient = me != null && me.isPatient();
        boolean isDoctor = me != null && me.isDoctor();
        canEdit = isPatient || (isDoctor && report.doctorEditPermission);

        String status = report.status != null ? report.status : "PENDING";

        b.tvFileName.setText(report.fileName != null ? report.fileName : "Report");
        applyBadgeColor(b.tvStatus, status.equals("ANALYZED") || status.equals("REVIEWED") ? "green" : "blue");
        b.tvStatus.setText(status);

        b.tvUploadDate.setText(formatDate(report.uploadedAt));

        String sev = report.getSeverityLevel();
        if (sev != null && !status.equals("PENDING") && !status.equals("ANALYZING")) {
            b.tvSeverity.setVisibility(View.VISIBLE);
            b.tvSeverity.setText(sev);
            switch (sev) {
                case "CRITICAL":
                    applyBadgeColor(b.tvSeverity, "red");
                    break;
                case "HIGH":
                    applyBadgeColor(b.tvSeverity, "orange");
                    break;
                case "MEDIUM":
                    applyBadgeColor(b.tvSeverity, "amber");
                    break;
                default:
                    applyBadgeColor(b.tvSeverity, "green");
                    break;
            }
        } else {
            b.tvSeverity.setVisibility(View.GONE);
        }

        if (report.medicalSpecialty != null && !report.medicalSpecialty.isEmpty()) {
            b.tvSpecialty.setVisibility(View.VISIBLE);
            b.tvSpecialty.setText(report.medicalSpecialty);
            applyBadgeColor(b.tvSpecialty, "blue");
        } else {
            b.tvSpecialty.setVisibility(View.GONE);
        }

        if (report.isPrivate != null && report.isPrivate) {
            b.tvPrivate.setVisibility(View.VISIBLE);
            applyBadgeColor(b.tvPrivate, "blue");
            b.tvPrivate.setText("Privately Shared");
        } else {
            b.tvPrivate.setVisibility(View.GONE);
        }

        b.layoutFileInfo.setVisibility(View.VISIBLE);
        b.tvFileType.setText(report.fileType != null ? report.fileType.toUpperCase() : "FILE");
        String fileSizeStr = "Unknown size";
        if (report.fileSize != null) {
            try {
                long bytes = Long.parseLong(report.fileSize);
                fileSizeStr = bytes > 1024 * 1024 ? String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
                        : String.format(Locale.US, "%.0f KB", bytes / 1024.0);
            } catch (Exception e) {
                fileSizeStr = report.fileSize;
            }
        }
        b.tvFileSize.setText(fileSizeStr);

        if (report.analyzedAt != null) {
            b.tvAnalyzedAt.setVisibility(View.VISIBLE);
            b.tvAnalyzedAt.setText("Analyzed: " + formatDate(report.analyzedAt));
        }

        JsonObject ai = smartParseObj(report.aiAnalysis);

        b.cardSummary.setVisibility(View.GONE);
        b.cardExtraction.setVisibility(View.GONE);
        b.cardPatientInfo.setVisibility(View.GONE);
        b.cardDiagnoses.setVisibility(View.GONE);
        b.cardSymptoms.setVisibility(View.GONE);
        b.cardVitals.setVisibility(View.GONE);
        b.cardLabResults.setVisibility(View.GONE);
        b.cardMedications.setVisibility(View.GONE);
        b.cardAbnormal.setVisibility(View.GONE);
        b.cardSuggestions.setVisibility(View.GONE);
        b.cardSuggestedDoctors.setVisibility(View.GONE);

        if (ai != null && !status.equals("ANALYZING") && !status.equals("PENDING")) {
            Log.d(TAG, "AI Analysis keys: " + ai.keySet());

            String summary = report.getAiSummary();
            if (summary != null && !summary.isEmpty()) {
                b.cardSummary.setVisibility(View.VISIBLE);
                markwon.setMarkdown(b.tvSummary, summary);
                String model = report.getModelUsed();
                if (model != null) {
                    b.tvModelUsed.setVisibility(View.VISIBLE);
                    b.tvModelUsed.setText("Model: " + model);
                }
            }

            JsonObject extraction = smartParseObj(ai.get("extraction_info"));
            if (extraction != null) {
                b.cardExtraction.setVisibility(View.VISIBLE);
                b.layoutExtractionBadges.removeAllViews();

                String ft = safeStr(extraction, "file_type", null);
                if (ft != null)
                    addExtractionBadge("📄 " + ft.toUpperCase(), "#F1F5F9", "@color/text_secondary");

                String tl = safeStr(extraction, "text_length", null);
                if (tl != null)
                    addExtractionBadge(tl + " chars", "#F1F5F9", "@color/text_secondary");

                boolean success = false;
                try {
                    success = extraction.get("extraction_successful").getAsBoolean();
                } catch (Exception e) {
                }
                addExtractionBadge(success ? "✅ Extracted" : "❌ Failed",
                        success ? "#F0FDF4" : "#FEF2F2", success ? "@color/badge_text_green" : "@color/badge_text_red");

                String ocrScore = safeStr(extraction, "ocr_quality_score", null);
                if (ocrScore != null) {
                    try {
                        int pct = (int) (Double.parseDouble(ocrScore) * 100);
                        addExtractionBadge("OCR " + pct + "%",
                                pct >= 80 ? "#F0FDF4" : pct >= 50 ? "#FFFBEB" : "#FEF2F2",
                                pct >= 80 ? "@color/badge_text_green"
                                        : pct >= 50 ? "@color/badge_text_amber" : "@color/badge_text_red");
                    } catch (Exception e) {
                    }
                }

                String preview = safeStr(extraction, "extracted_text_preview", null);
                if (preview != null && !preview.isEmpty()) {
                    b.tvExtractionPreview.setVisibility(View.VISIBLE);
                    b.tvExtractionPreview.setText(preview);
                }
            }

            JsonObject pi = smartParseObj(ai.get("patient_info"));
            if (pi != null && pi.entrySet().size() > 0) {
                b.cardPatientInfo.setVisibility(View.VISIBLE);
                b.layoutPatientGrid.removeAllViews();
                if (canEdit)
                    injectEditButton(b.cardPatientInfo, "patient_info");
                addPatientField(b.layoutPatientGrid, "Name", safeStr(pi, "name", null), "blue");
                String age = safeStr(pi, "age", null);
                String gender = safeStr(pi, "gender", null);
                if (age != null || gender != null) {
                    String combined = (age != null ? age + " years" : "") + (gender != null ? " • " + gender : "");
                    addPatientField(b.layoutPatientGrid, "Age / Gender", combined.trim(), "green");
                }
                addPatientField(b.layoutPatientGrid, "Blood Group", safeStr(pi, "blood_group", null), "red");
                addPatientField(b.layoutPatientGrid, "Patient ID", safeStr(pi, "patient_id", null), "blue");
                addPatientField(b.layoutPatientGrid, "Contact", safeStr(pi, "contact", null), "blue");
                addPatientField(b.layoutPatientGrid, "Address", safeStr(pi, "address", null), "blue");
            }

            JsonArray diagnoses = smartParseArray(ai.get("diagnoses"));
            if (diagnoses != null) {
                b.cardDiagnoses.setVisibility(View.VISIBLE);
                if (canEdit)
                    injectEditButton(b.cardDiagnoses, "diagnoses");
                b.tvDiagnosesCount.setText(diagnoses.size() + " identified");
                if (diagnoses.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement e : diagnoses) {
                        String displayDiag = com.medreport.ai.utils.MedicalDataUtils.getDisplayString(e);
                        sb.append("• ").append(displayDiag).append("\n");
                    }
                    b.tvDiagnoses.setText(sb.toString().trim());
                } else {
                    b.tvDiagnoses.setText("No diagnoses identified in this report.");
                    b.tvDiagnoses.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                }
            }

            JsonArray symptoms = smartParseArray(ai.get("symptoms"));
            if (symptoms != null) {
                b.cardSymptoms.setVisibility(View.VISIBLE);
                if (canEdit)
                    injectEditButton(b.cardSymptoms, "symptoms");
                if (symptoms.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement e : symptoms) {
                        try {
                            sb.append("• ").append(e.getAsString()).append("\n");
                        } catch (Exception ex) {
                            sb.append("• ").append(e.toString()).append("\n");
                        }
                    }
                    b.tvSymptoms.setText(sb.toString().trim());
                } else {
                    b.tvSymptoms.setText("No symptoms reported.");
                    b.tvSymptoms.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                }
            }

            JsonObject vitals = smartParseObj(ai.get("vital_signs"));
            if (vitals != null) {
                b.gridVitals.removeAllViews();
                if (canEdit)
                    injectEditButton(b.cardVitals, "vital_signs");
                boolean hasAnyVital = false;
                for (Map.Entry<String, JsonElement> entry : vitals.entrySet()) {
                    String val = "";
                    try {
                        val = entry.getValue().isJsonNull() ? "" : entry.getValue().getAsString();
                    } catch (Exception ex) {
                        val = "";
                    }
                    if (val.isEmpty() || val.equals("null"))
                        continue;
                    hasAnyVital = true;
                    addVitalItem(b.gridVitals, formatKey(entry.getKey()), val);
                }
                b.cardVitals.setVisibility(hasAnyVital ? View.VISIBLE : View.GONE);
            }

            JsonArray labs = smartParseArray(ai.get("lab_results"));
            if (labs != null) {
                b.cardLabResults.setVisibility(View.VISIBLE);
                if (canEdit)
                    injectEditButton(b.cardLabResults, "lab_results");
                b.tvLabCount.setText(labs.size() + " tests analyzed");
                b.layoutLabItems.removeAllViews();
                if (labs.size() > 0) {
                    for (JsonElement e : labs) {
                        if (e.isJsonObject())
                            addLabItem(b.layoutLabItems, e.getAsJsonObject());
                    }
                } else {
                    TextView tv = new TextView(this);
                    tv.setText("No lab tests found in this report.");
                    tv.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                    tv.setTextSize(13);
                    b.layoutLabItems.addView(tv);
                }
            }

            JsonArray meds = smartParseArray(ai.get("current_medications"));
            if (meds != null) {
                b.cardMedications.setVisibility(View.VISIBLE);
                if (canEdit)
                    injectEditButton(b.cardMedications, "medications");
                if (meds.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement e : meds) {
                        if (e.isJsonObject()) {
                            JsonObject obj = e.getAsJsonObject();
                            String mName = safeStr(obj, "name", safeStr(obj, "medication", "Unknown"));
                            String mDose = safeStr(obj, "dosage", safeStr(obj, "dose", ""));
                            String freq = safeStr(obj, "frequency", "");
                            sb.append("• ").append(mName);
                            if (mDose != null && !mDose.isEmpty())
                                sb.append("  —  ").append(mDose);
                            if (freq != null && !freq.isEmpty())
                                sb.append("  (").append(freq).append(")");
                            sb.append("\n");
                        } else {
                            try {
                                sb.append("• ").append(e.getAsString()).append("\n");
                            } catch (Exception ex) {
                            }
                        }
                    }
                    b.tvMedications.setText(sb.toString().trim());
                } else {
                    b.tvMedications.setText("No medications identified.");
                    b.tvMedications.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                }
            }

            JsonArray abnormal = smartParseArray(ai.get("abnormal_findings"));
            if (abnormal != null && abnormal.size() > 0) {
                b.cardAbnormal.setVisibility(View.VISIBLE);
                StringBuilder sb = new StringBuilder();
                for (JsonElement e : abnormal) {
                    if (e.isJsonObject()) {
                        JsonObject obj = e.getAsJsonObject();
                        String finding = safeStr(obj, "finding", safeStr(obj, "test_name", e.toString()));
                        String significance = safeStr(obj, "significance", safeStr(obj, "severity", ""));
                        sb.append("⚠ ").append(finding);
                        if (significance != null && !significance.isEmpty())
                            sb.append("  [").append(significance).append("]");
                        sb.append("\n");
                    } else {
                        try {
                            sb.append("⚠ ").append(e.getAsString()).append("\n");
                        } catch (Exception ex) {
                        }
                    }
                }
                b.tvAbnormal.setText(sb.toString().trim());
            }

            JsonArray suggestions = smartParseArray(ai.get("clinical_suggestions"));
            if (suggestions != null && suggestions.size() > 0) {
                b.cardSuggestions.setVisibility(View.VISIBLE);
                b.layoutSuggestionItems.removeAllViews();
                for (int i = 0; i < suggestions.size(); i++) {
                    JsonElement e = suggestions.get(i);
                    if (e.isJsonObject()) {
                        addSuggestionItem(b.layoutSuggestionItems, e.getAsJsonObject(), i);
                    } else {
                        try {
                            addSimpleText(b.layoutSuggestionItems, (i + 1) + ". " + e.getAsString());
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }

        if (report.suggestedDoctors != null && !report.suggestedDoctors.isEmpty() && isPatient) {
            b.cardSuggestedDoctors.setVisibility(View.VISIBLE);
            b.layoutDoctorItems.removeAllViews();
            for (int i = 0; i < report.suggestedDoctors.size(); i++) {
                addDoctorItem(b.layoutDoctorItems, report.suggestedDoctors.get(i), i);
            }
        }

        if (report.doctorNotes != null && !report.doctorNotes.isEmpty()) {
            b.cardDoctorNotes.setVisibility(View.VISIBLE);
            b.tvDoctorNotes.setText(report.doctorNotes);
        } else {
            b.cardDoctorNotes.setVisibility(View.GONE);
        }

        if (isDoctor) {
            // b.btnChat is legacy dummy View
        } else {
            // b.btnChat is legacy dummy View
        }

        b.btnChat.setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra(ChatActivity.EXTRA_REPORT_ID, reportId);
            startActivity(i);
        });

        if (isPatient) {
            if (!status.equals("ANALYZING")) {
                b.btnAnalyze.setVisibility(View.VISIBLE);
                b.btnDelete.setVisibility(View.VISIBLE);
            }
            if (report.isAnalyzed()) {
                b.btnAskAI.setVisibility(View.VISIBLE);
            } else {
                b.btnAskAI.setVisibility(View.GONE);
            }
            b.switchDoctorEdit.setVisibility(report.assignedDoctorId != null ? View.VISIBLE : View.GONE);
        } else {
            b.btnAnalyze.setVisibility(View.GONE);
            b.btnDelete.setVisibility(View.GONE);
            b.switchDoctorEdit.setVisibility(View.GONE);
        }

        b.btnDelete.setOnClickListener(v -> confirmDelete());
        b.btnAnalyze.setOnClickListener(v -> triggerAnalysis());
        b.btnAskAI.setOnClickListener(v -> {
            com.medreport.ai.fragments.AIChatBottomSheet.newInstance(reportId)
                    .show(getSupportFragmentManager(), "ai_chat");
        });

        b.btnExportPdf.setVisibility(report.isAnalyzed() ? View.VISIBLE : View.GONE);
        b.btnExportPdf.setOnClickListener(v -> downloadAnalysisPdf());

        b.switchDoctorEdit.setChecked(report.doctorEditPermission);
        b.switchDoctorEdit.setOnCheckedChangeListener((sw, checked) -> {
            if (!sw.isPressed())
                return;
            Map<String, Boolean> body = new HashMap<>();
            body.put("allow", checked);
            ApiClient.get().setDoctorEditPermission(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
                @Override
                public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                    Toast.makeText(ReportDetailActivity.this, checked ? "Doctor can now edit" : "Edit access revoked",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                }
            });
        });

        b.btnViewOriginal.setVisibility(View.GONE);
        b.btnViewOriginal.setOnClickListener(v -> viewOriginalReport());

        b.cardReview.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
        b.btnSubmitReview.setOnClickListener(v -> submitReview());

        // --- Console/Bottom Menu Logic ---
        setupQuickActions();
    }

    private void setupQuickActions() {
        if (report == null) return;
        
        UserModel me = AuthManager.getInstance().getCurrentUser();
        boolean isPatient = me != null && me.isPatient();
        boolean isDoctor = me != null && me.isDoctor();
        String status = report.status != null ? report.status : "PENDING";
        boolean isAnalyzing = "ANALYZING".equals(status);
        boolean isAnalyzed = report.isAnalyzed();

        // 1. Primary Button Logic
        if (isPatient) {
            if (isAnalyzing) {
                b.btnAnalyzePrimary.setVisibility(View.VISIBLE);
                b.btnAnalyzePrimary.setText("Analyzing...");
                b.btnAnalyzePrimary.setEnabled(false);
                b.btnAskAIPrimary.setVisibility(View.GONE);
            } else if (!isAnalyzed) {
                b.btnAnalyzePrimary.setVisibility(View.VISIBLE);
                b.btnAnalyzePrimary.setText("Start AI Analysis");
                b.btnAnalyzePrimary.setEnabled(true);
                b.btnAskAIPrimary.setVisibility(View.GONE);
            } else {
                b.btnAnalyzePrimary.setVisibility(View.GONE);
                b.btnAskAIPrimary.setVisibility(View.VISIBLE);
            }
        } else {
            b.btnAnalyzePrimary.setVisibility(View.GONE);
            b.btnAskAIPrimary.setVisibility(View.GONE);
        }

        b.btnAnalyzePrimary.setOnClickListener(v -> triggerAnalysis());
        b.btnAskAIPrimary.setOnClickListener(v -> {
            com.medreport.ai.fragments.AIChatBottomSheet.newInstance(reportId)
                    .show(getSupportFragmentManager(), "ai_chat");
        });

        // 2. Quick Actions Menu Preparation
        b.btnQuickActions.setOnClickListener(v -> {
            ReportActionsBottomSheet sheet = ReportActionsBottomSheet.newInstance();
            
            // Add contextual items
            sheet.addAction("original", "View Original", R.drawable.ic_file, R.color.primary);
            
            if (isAnalyzed) {
                sheet.addAction("export", "Export PDF", R.drawable.ic_document, R.color.success);
            }
            
            if (isPatient && isAnalyzed) {
                sheet.addAction("ask_ai", "Ask AI Assistant", R.drawable.ic_chat, R.color.badge_text_blue);
            }
            
            if (isPatient && !isAnalyzing) {
                sheet.addAction("reanalyze", "Re-analyze Report", R.drawable.ic_clock, R.color.warning);
            }
            
            if (isDoctor || (isPatient && report.assignedDoctorId != null)) {
                sheet.addAction("chat", isDoctor ? "Chat with Patient" : "Chat with Doctor", R.drawable.ic_chat, R.color.primary);
            }
            
            if (isPatient && !isAnalyzing) {
                sheet.addAction("delete", "Delete Report", R.drawable.ic_delete, R.color.danger);
            }
            
            sheet.setListener(action -> {
                switch (action) {
                    case "original": viewOriginalReport(); break;
                    case "export": downloadAnalysisPdf(); break;
                    case "ask_ai": b.btnAskAIPrimary.performClick(); break;
                    case "reanalyze": triggerAnalysis(); break;
                    case "chat": b.btnChat.performClick(); break;
                    case "delete": confirmDelete(); break;
                }
            });
            
            sheet.show(getSupportFragmentManager(), "quick_actions");
        });

        // Show/Hide bottom bar entirely if no primary or quick actions are relevant
        // (Simplified check: always show if there's a reason for QuickActions, which 'viewOriginal' always is)
        b.layoutBottomActions.setVisibility(View.VISIBLE);
    }

    private void downloadAnalysisPdf() {
        if (report == null || reportId == null) return;

        String url = com.medreport.ai.BuildConfig.BASE_URL + "medical-reports/" + reportId + "/export-pdf";
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        
        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(url));
        request.setTitle("MedReport Analysis");
        request.setDescription("Downloading professional analysis for " + report.fileName);
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        
        String fileName = "MedReport_Analysis_" + reportId.substring(0, 8) + ".pdf";
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);

        // Add verification token
        String token = AuthManager.getInstance().getCachedToken();
        if (token != null && !token.isEmpty()) {
            request.addRequestHeader("Authorization", "Bearer " + token);
        }

        android.app.DownloadManager manager = (android.app.DownloadManager) getSystemService(android.content.Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(this, "Download started. Check your notifications.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Download manager not available", Toast.LENGTH_SHORT).show();
        }
    }


    
    private void injectEditButton(com.google.android.material.card.MaterialCardView card, String sectionKey) {
        LinearLayout cardRoot = null;
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof LinearLayout) {
                cardRoot = (LinearLayout) child;
                break;
            }
        }
        if (cardRoot == null)
            return;

        LinearLayout headerRow = null;
        for (int i = 0; i < cardRoot.getChildCount(); i++) {
            View child = cardRoot.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) child;
                if (ll.getOrientation() == LinearLayout.HORIZONTAL) {
                    headerRow = ll;
                    break;
                }
            }
        }
        if (headerRow == null)
            return;

        if (headerRow.getTag() != null && headerRow.getTag().equals("edit_injected"))
            return;
        headerRow.setTag("edit_injected");

        TextView btnEdit = new TextView(this);
        btnEdit.setText("Edit");
        btnEdit.setTextSize(12);
        btnEdit.setTypeface(null, Typeface.BOLD);
        btnEdit.setTextColor(ContextCompat.getColor(this, R.color.primary));
        btnEdit.setBackgroundResource(R.drawable.bg_badge);
        btnEdit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.badge_bg_blue)));
        int hp = dpToPx(12);
        int vp = dpToPx(6);
        btnEdit.setPadding(hp, vp, hp, vp);
        btnEdit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0);
        btnEdit.setCompoundDrawablePadding(dpToPx(4));
        btnEdit.setCompoundDrawableTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        btnEdit.setOnClickListener(v -> openEditDialog(sectionKey));
        headerRow.addView(btnEdit);
    }

    private void openEditDialog(String sectionKey) {
        if (report == null || report.aiAnalysis == null || !report.aiAnalysis.isJsonObject())
            return;
        new EditSectionDialog(this, sectionKey, report.aiAnalysis.getAsJsonObject(), (section, updatedAnalysis) -> {
            saveEditedAnalysis(updatedAnalysis);
        }).show();
    }

    private void saveEditedAnalysis(JsonObject updatedAnalysis) {
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();

        Map<String, Object> body = new HashMap<>();
        body.put("ai_analysis", new com.google.gson.Gson().fromJson(updatedAnalysis, Map.class));

        ApiClient.get().updateAiAnalysis(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    bindReport();
                    Toast.makeText(ReportDetailActivity.this, "Analysis updated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReportDetailActivity.this, "Failed to save changes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                Log.e(TAG, "Save edit failed", t);
                Toast.makeText(ReportDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void addPatientField(LinearLayout container, String label, String value, String color) {
        if (value == null || value.isEmpty() || value.equals("null"))
            return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_badge);
        int bgColor;
        int txtColor;
        switch (color) {
            case "green":
                bgColor = ContextCompat.getColor(this, R.color.badge_bg_green);
                txtColor = ContextCompat.getColor(this, R.color.badge_text_green);
                break;
            case "red":
                bgColor = ContextCompat.getColor(this, R.color.badge_bg_red);
                txtColor = ContextCompat.getColor(this, R.color.badge_text_red);
                break;
            default:
                bgColor = ContextCompat.getColor(this, R.color.badge_bg_blue);
                txtColor = ContextCompat.getColor(this, R.color.badge_text_blue);
                break;
        }
        row.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        int pad = dpToPx(12);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(8);
        row.setLayoutParams(lp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label.toUpperCase());
        tvLabel.setTextSize(10);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setLetterSpacing(0.05f);
        tvLabel.setTextColor(txtColor);
        row.addView(tvLabel);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(15);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvVal.setPadding(0, dpToPx(4), 0, 0);
        row.addView(tvVal);

        container.addView(row);
    }

    private void addExtractionBadge(String text, String bgHex, String textColorRes) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(11);
        badge.setBackgroundResource(R.drawable.bg_badge);
        badge.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.parseColor(bgHex)));
        int txtColor;
        switch (textColorRes) {
            case "@color/badge_text_green":
                txtColor = ContextCompat.getColor(this, R.color.badge_text_green);
                break;
            case "@color/badge_text_red":
                txtColor = ContextCompat.getColor(this, R.color.badge_text_red);
                break;
            case "@color/badge_text_amber":
                txtColor = ContextCompat.getColor(this, R.color.badge_text_amber);
                break;
            default:
                txtColor = ContextCompat.getColor(this, R.color.text_secondary);
                break;
        }
        badge.setTextColor(txtColor);
        badge.setTypeface(null, Typeface.BOLD);
        int hp = dpToPx(8);
        int vp = dpToPx(4);
        badge.setPadding(hp, vp, hp, vp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dpToPx(8);
        lp.bottomMargin = dpToPx(4);
        badge.setLayoutParams(lp);
        b.layoutExtractionBadges.addView(badge);
    }

    private void addVitalItem(GridLayout grid, String label, String value) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setBackgroundResource(R.drawable.bg_badge);
        item.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.badge_bg_green)));
        int pad = dpToPx(12);
        item.setPadding(pad, pad, pad, pad);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        lp.width = 0;
        item.setLayoutParams(lp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label.toUpperCase());
        tvLabel.setTextSize(10);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.badge_text_green));
        tvLabel.setLetterSpacing(0.05f);
        item.addView(tvLabel);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(16);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        item.addView(tvVal);

        grid.addView(item);
    }

    private void addLabItem(LinearLayout container, JsonObject lab) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_badge);
        row.setBackgroundTintList(ColorStateList.valueOf(0x08000000));
        int pad = dpToPx(12);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dpToPx(8);
        row.setLayoutParams(rlp);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        String testName = safeStr(lab, "test_name", safeStr(lab, "name", "Test"));
        TextView tvName = new TextView(this);
        tvName.setText(testName);
        tvName.setTextSize(14);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        left.addView(tvName);

        String value = safeStr(lab, "value", "");
        String unit = safeStr(lab, "unit", safeStr(lab, "units", ""));
        String refRange = safeStr(lab, "reference_range", safeStr(lab, "normal_range", ""));

        if (value != null && !value.isEmpty()) {
            TextView tvVal = new TextView(this);
            tvVal.setText("Value: " + value + (unit != null && !unit.isEmpty() ? " " + unit : ""));
            tvVal.setTextSize(12);
            tvVal.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            left.addView(tvVal);
        }
        if (refRange != null && !refRange.isEmpty()) {
            TextView tvRef = new TextView(this);
            tvRef.setText("Ref: " + refRange);
            tvRef.setTextSize(11);
            tvRef.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            left.addView(tvRef);
        }
        row.addView(left);

        String labStatus = safeStr(lab, "status", safeStr(lab, "flag", ""));
        if (labStatus != null && !labStatus.isEmpty()) {
            TextView badge = new TextView(this);
            badge.setText(labStatus.toUpperCase());
            badge.setTextSize(10);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setBackgroundResource(R.drawable.bg_badge);
            badge.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            boolean isAbn = labStatus.equalsIgnoreCase("abnormal") || labStatus.equalsIgnoreCase("high")
                    || labStatus.equalsIgnoreCase("low") || labStatus.equalsIgnoreCase("critical");
            applyBadgeColor(badge, isAbn ? "red" : "green");
            row.addView(badge);
        }
        container.addView(row);
    }

    private void addSuggestionItem(LinearLayout container, JsonObject s, int index) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setBackgroundResource(R.drawable.bg_badge);
        item.setBackgroundTintList(ColorStateList.valueOf(0x08000000));
        int pad = dpToPx(14);
        item.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(10);
        item.setLayoutParams(lp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        String priority = safeStr(s, "priority", "MEDIUM");
        TextView tvPri = new TextView(this);
        tvPri.setText(priority);
        tvPri.setTextSize(10);
        tvPri.setTypeface(null, Typeface.BOLD);
        tvPri.setBackgroundResource(R.drawable.bg_badge);
        tvPri.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        switch (priority) {
            case "CRITICAL":
                applyBadgeColor(tvPri, "red");
                break;
            case "HIGH":
                applyBadgeColor(tvPri, "orange");
                break;
            case "MEDIUM":
                applyBadgeColor(tvPri, "amber");
                break;
            default:
                applyBadgeColor(tvPri, "green");
                break;
        }
        header.addView(tvPri);

        String title = safeStr(s, "suggestion",
                safeStr(s, "title", safeStr(s, "recommendation", "Suggestion " + (index + 1))));
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dpToPx(10);
        tvTitle.setLayoutParams(tlp);
        header.addView(tvTitle);
        item.addView(header);

        String category = safeStr(s, "category", null);
        if (category != null && !category.isEmpty()) {
            TextView tvCat = new TextView(this);
            tvCat.setText("Category: " + category);
            tvCat.setTextSize(12);
            tvCat.setTypeface(null, Typeface.BOLD);
            tvCat.setTextColor(ContextCompat.getColor(this, R.color.badge_text_blue));
            tvCat.setPadding(0, dpToPx(6), 0, 0);
            item.addView(tvCat);
        }

        String reason = safeStr(s, "reasoning", safeStr(s, "reason", safeStr(s, "rationale", null)));
        if (reason != null && !reason.isEmpty()) {
            TextView tvReason = new TextView(this);
            tvReason.setText(reason);
            tvReason.setTextSize(13);
            tvReason.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvReason.setPadding(0, dpToPx(4), 0, 0);
            item.addView(tvReason);
        }

        String confStr = safeStr(s, "confidence", null);
        if (confStr != null) {
            try {
                double conf = Double.parseDouble(confStr);
                TextView tvConf = new TextView(this);
                tvConf.setText("AI Confidence: " + Math.round(conf * 100) + "%");
                tvConf.setTextSize(11);
                tvConf.setTypeface(null, Typeface.BOLD);
                tvConf.setTextColor(ContextCompat.getColor(this, R.color.accent));
                tvConf.setPadding(0, dpToPx(4), 0, 0);
                item.addView(tvConf);
            } catch (Exception ex) {
            }
        }

        container.addView(item);
    }

    private void addDoctorItem(LinearLayout container, ReportModel.SuggestedDoctor doc, int index) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        item.setBackgroundResource(R.drawable.bg_badge);
        item.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.badge_bg_green)));
        int pad = dpToPx(14);
        item.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(8);
        item.setLayoutParams(lp);

        TextView tvRank = new TextView(this);
        tvRank.setText("#" + (index + 1));
        tvRank.setTextSize(12);
        tvRank.setTypeface(null, Typeface.BOLD);
        tvRank.setTextColor(0xFFFFFFFF);
        tvRank.setGravity(android.view.Gravity.CENTER);
        tvRank.setBackgroundResource(R.drawable.bg_dot_green);
        tvRank.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        rlp.rightMargin = dpToPx(12);
        tvRank.setLayoutParams(rlp);
        item.addView(tvRank);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText("Dr. " + (doc.doctorName != null ? doc.doctorName : "Unknown"));
        tvName.setTextSize(15);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        info.addView(tvName);

        int score = (int) (doc.matchScore * 100);
        TextView tvScore = new TextView(this);
        tvScore.setText("Match: " + score + "%");
        tvScore.setTextSize(12);
        tvScore.setTypeface(null, Typeface.BOLD);
        tvScore.setTextColor(ContextCompat.getColor(this, R.color.badge_text_green));
        info.addView(tvScore);

        if (doc.specializations != null && !doc.specializations.isEmpty()) {
            TextView tvSpecs = new TextView(this);
            StringBuilder sb = new StringBuilder();
            for (String spec : doc.specializations)
                sb.append(spec).append(", ");
            tvSpecs.setText(sb.toString().replaceAll(", $", ""));
            tvSpecs.setTextSize(11);
            tvSpecs.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            info.addView(tvSpecs);
        }
        item.addView(info);

        if (report.assignedDoctorId == null) {
            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);

            com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(this);
            btn.setText("Assign");
            btn.setTextSize(10);
            btn.setAllCaps(false);
            btn.setCornerRadius(dpToPx(8));
            btn.setMinimumHeight(dpToPx(32));
            btn.setPadding(dpToPx(8), 0, dpToPx(8), 0);
            btn.setOnClickListener(v -> assignDoctor(doc.doctorId, doc.doctorName));
            buttons.addView(btn);

            com.google.android.material.button.MaterialButton btnPv = new com.google.android.material.button.MaterialButton(this);
            btnPv.setText("Private");
            btnPv.setTextSize(10);
            btnPv.setAllCaps(false);
            btnPv.setCornerRadius(dpToPx(8));
            btnPv.setMinimumHeight(dpToPx(32));
            btnPv.setPadding(dpToPx(8), 0, dpToPx(8), 0);
            btnPv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_dark)));
            btnPv.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_lock));
            btnPv.setIconPadding(dpToPx(4));
            btnPv.setIconGravity(com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START);
            btnPv.setOnClickListener(v -> privateAssignDoctor(doc.doctorId, doc.doctorName));
            LinearLayout.LayoutParams lpPv = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpPv.leftMargin = dpToPx(6);
            btnPv.setLayoutParams(lpPv);
            buttons.addView(btnPv);

            item.addView(buttons);
        } else if (doc.doctorId.equals(report.assignedDoctorId) && report.isPrivate != null && report.isPrivate) {
            TextView tvPv = new TextView(this);
            tvPv.setText("PRIVATELY SHARED");
            tvPv.setTextSize(9);
            tvPv.setTypeface(null, Typeface.BOLD);
            tvPv.setTextColor(ContextCompat.getColor(this, R.color.badge_text_blue));
            tvPv.setBackgroundResource(R.drawable.bg_badge);
            tvPv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.badge_bg_blue)));
            tvPv.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            item.addView(tvPv);
        }

        container.addView(item);
    }

    private void addSimpleText(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tv.setPadding(0, 0, 0, dpToPx(8));
        container.addView(tv);
    }


    private void triggerAnalysis() {
        b.btnAnalyzePrimary.setEnabled(false);
        b.btnAnalyzePrimary.setText("Starting...");
        ApiClient.get().analyzeReport(reportId).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    bindReport();
                    
                    if ("ANALYZED".equals(report.status)) {
                        stopPolling();
                        Toast.makeText(ReportDetailActivity.this, "Analysis complete!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ReportDetailActivity.this, "AI Analysis started!", Toast.LENGTH_SHORT).show();
                        startPolling();
                    }
                } else {
                    b.btnAnalyzePrimary.setEnabled(true);
                    b.btnAnalyzePrimary.setText("Start AI Analysis");
                    Toast.makeText(ReportDetailActivity.this, "Failed to start", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                b.btnAnalyzePrimary.setEnabled(true);
                b.btnAnalyzePrimary.setText("Start AI Analysis");
            }
        });
    }

    private void assignDoctor(String doctorId, String doctorName) {
        Map<String, String> body = new HashMap<>();
        body.put("doctor_id", doctorId);
        ApiClient.get().assignDoctor(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(ReportDetailActivity.this, "Dr. " + doctorName + " assigned!", Toast.LENGTH_SHORT)
                            .show();
                    loadReport();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                Toast.makeText(ReportDetailActivity.this, "Assignment failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void privateAssignDoctor(String doctorId, String doctorName) {
        new AlertDialog.Builder(this)
                .setTitle("Private Consultation")
                .setMessage("Share this report privately with Dr. " + doctorName + "? It will not be visible to other doctors. This will also start a private chat.")
                .setPositiveButton("Share Privately", (dialog, which) -> {
                    Map<String, String> body = new HashMap<>();
                    body.put("doctor_id", doctorId);
                    ApiClient.get().privateAssignDoctor(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(ReportDetailActivity.this, "Shared privately with Dr. " + doctorName, Toast.LENGTH_SHORT).show();
                                loadReport();
                            } else {
                                Toast.makeText(ReportDetailActivity.this, "Assignment failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                            Toast.makeText(ReportDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReview() {
        String notes = b.etReviewNotes.getText().toString().trim();
        Map<String, String> body = new HashMap<>();
        body.put("notes", notes);
        ApiClient.get().reviewReport(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(ReportDetailActivity.this, "Review saved!", Toast.LENGTH_SHORT).show();
                    loadReport();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this).setTitle("Delete Report")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    ApiClient.get().deleteReport(reportId).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                            Toast.makeText(ReportDetailActivity.this, "Report deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {
                        }
                    });
                }).setNegativeButton("Cancel", null).show();
    }

    private void viewOriginalReport() {
        if (report == null) return;

        if (report.fileUrl != null && !report.fileUrl.isEmpty()) {
            openFileUrl(report.fileUrl);
        } else {
            downloadAndOpenFile();
        }
    }

    private void openFileUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening URL", e);
            Toast.makeText(this, "Failed to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAndOpenFile() {
        Toast.makeText(this, "Loading original report...", Toast.LENGTH_SHORT).show();

        ApiClient.get().downloadReport(reportId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        java.io.File cacheDir = getCacheDir();
                        String fileName = report.fileName != null ? report.fileName : "report_" + reportId;
                        java.io.File file = new java.io.File(cacheDir, fileName);

                        java.io.InputStream inputStream = response.body().byteStream();
                        java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.close();
                        inputStream.close();

                        openFile(file);
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving file", e);
                        Toast.makeText(ReportDetailActivity.this, "Failed to open file", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ReportDetailActivity.this, "File not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Log.e(TAG, "Download failed", t);
                Toast.makeText(ReportDetailActivity.this, "File not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFile(java.io.File file) {
        try {
            android.net.Uri uri;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".provider",
                        file
                );
            } else {
                uri = android.net.Uri.fromFile(file);
            }

            String mimeType = getMimeType(file.getName());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "No app found to open this file type", Toast.LENGTH_LONG).show();
                shareFile(uri, mimeType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening file", e);
            Toast.makeText(this, "Failed to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(android.net.Uri uri, String mimeType) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Open with"));
    }

    private String getMimeType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }

        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt":
                return "text/plain";
            default:
                return "*/*";
        }
    }


    private void applyBadgeColor(TextView tv, String color) {
        int bgRes, txtRes;
        switch (color) {
            case "red":
                bgRes = R.color.badge_bg_red;
                txtRes = R.color.badge_text_red;
                break;
            case "orange":
                bgRes = R.color.badge_bg_orange;
                txtRes = R.color.badge_text_orange;
                break;
            case "amber":
                bgRes = R.color.badge_bg_amber;
                txtRes = R.color.badge_text_amber;
                break;
            case "green":
                bgRes = R.color.badge_bg_green;
                txtRes = R.color.badge_text_green;
                break;
            default:
                bgRes = R.color.badge_bg_blue;
                txtRes = R.color.badge_text_blue;
                break;
        }
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, bgRes)));
        tv.setTextColor(ContextCompat.getColor(this, txtRes));
    }

    private String formatKey(String key) {
        String s = key.replace("_", " ");
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String formatDate(String iso) {
        if (iso == null)
            return "";
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = sdfIn.parse(iso);
            if (d != null)
                return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) {
        }
        return iso;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private JsonObject smartParseObj(JsonElement el) {
        if (el == null || el.isJsonNull())
            return null;
        if (el.isJsonObject())
            return el.getAsJsonObject();
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            try {
                String str = el.getAsString().trim().replace("'", "\"").replace("None", "null")
                        .replace("False", "false").replace("True", "true");
                return new JsonParser().parse(str).getAsJsonObject();
            } catch (Exception e) {
                Log.e(TAG, "Obj parse fail", e);
            }
        }
        return null;
    }

    private JsonArray smartParseArray(JsonElement el) {
        if (el == null || el.isJsonNull())
            return null;
        if (el.isJsonArray())
            return el.getAsJsonArray();
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            try {
                String str = el.getAsString().trim().replace("'", "\"");
                return new JsonParser().parse(str).getAsJsonArray();
            } catch (Exception e) {
                Log.e(TAG, "Array parse fail", e);
            }
        }
        return null;
    }

    private String safeStr(JsonObject obj, String key, String fallback) {
        if (obj == null)
            return fallback;
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull())
                return obj.get(key).getAsString();
        } catch (Exception e) {
        }
        return fallback;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
