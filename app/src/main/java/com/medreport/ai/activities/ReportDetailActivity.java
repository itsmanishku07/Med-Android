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
        
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        b.swipeRefresh.setOnRefreshListener(() -> { stopPolling(); loadReport(); });
        loadReport();
    }

    // ══════════════════════════════════════════════════════════════════
    // DATA LOADING
    // ══════════════════════════════════════════════════════════════════

    private void loadReport() {
        b.swipeRefresh.setRefreshing(true);
        ApiClient.get().getReport(reportId).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                b.swipeRefresh.setRefreshing(false);
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    Log.d(TAG, "Report loaded. Status=" + report.status + " aiAnalysis=" + (report.aiAnalysis != null ? "present" : "null"));
                    bindReport();
                    if ("ANALYZING".equals(report.status)) startPolling(); else stopPolling();
                } else {
                    Log.e(TAG, "Load failed. Code=" + r.code());
                    Toast.makeText(ReportDetailActivity.this, "Failed to load report", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                b.swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Network error", t);
                Toast.makeText(ReportDetailActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    // POLLING
    // ══════════════════════════════════════════════════════════════════

    private void startPolling() {
        if (isPolling) return;
        isPolling = true;
        b.btnAnalyze.setEnabled(false);
        b.btnAnalyze.setText("Analyzing...");
        pollRunnable = () -> ApiClient.get().getReport(reportId).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    bindReport();
                    if ("ANALYZING".equals(report.status)) {
                        if (isPolling) pollHandler.postDelayed(pollRunnable, 5000);
                    } else {
                        stopPolling();
                        Toast.makeText(ReportDetailActivity.this, "Analysis complete!", Toast.LENGTH_SHORT).show();
                        loadReport(); // Force fresh reload
                    }
                } else if (isPolling) pollHandler.postDelayed(pollRunnable, 5000);
            }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                if (isPolling) pollHandler.postDelayed(pollRunnable, 5000);
            }
        });
        pollHandler.postDelayed(pollRunnable, 5000);
    }

    private void stopPolling() {
        isPolling = false;
        if (pollRunnable != null) pollHandler.removeCallbacks(pollRunnable);
        b.btnAnalyze.setEnabled(true);
        b.btnAnalyze.setText("Re-analyze with AI");
    }

    @Override protected void onDestroy() { super.onDestroy(); stopPolling(); }

    // ══════════════════════════════════════════════════════════════════
    // BIND ALL DATA FROM API RESPONSE
    // ══════════════════════════════════════════════════════════════════

    private void bindReport() {
        if (report == null) return;
        
        UserModel me = AuthManager.getInstance().getCurrentUser();
        boolean isPatient = me != null && me.isPatient();
        boolean isDoctor  = me != null && me.isDoctor();
        canEdit = isPatient || (isDoctor && report.doctorEditPermission);

        String status = report.status != null ? report.status : "PENDING";

        // ── 1. HEADER ──
        b.tvFileName.setText(report.fileName != null ? report.fileName : "Report");
        applyBadgeColor(b.tvStatus, status.equals("ANALYZED") || status.equals("REVIEWED") ? "green" : "blue");
        b.tvStatus.setText(status);

        // Upload date
        b.tvUploadDate.setText(formatDate(report.uploadedAt));

        // Severity badge
        String sev = report.getSeverityLevel();
        if (sev != null && !status.equals("PENDING") && !status.equals("ANALYZING")) {
            b.tvSeverity.setVisibility(View.VISIBLE);
            b.tvSeverity.setText(sev);
            switch (sev) {
                case "CRITICAL": applyBadgeColor(b.tvSeverity, "red"); break;
                case "HIGH":     applyBadgeColor(b.tvSeverity, "orange"); break;
                case "MEDIUM":   applyBadgeColor(b.tvSeverity, "amber"); break;
                default:         applyBadgeColor(b.tvSeverity, "green"); break;
            }
        } else {
            b.tvSeverity.setVisibility(View.GONE);
        }

        // Medical specialty badge
        if (report.medicalSpecialty != null && !report.medicalSpecialty.isEmpty()) {
            b.tvSpecialty.setVisibility(View.VISIBLE);
            b.tvSpecialty.setText(report.medicalSpecialty);
            applyBadgeColor(b.tvSpecialty, "blue");
        } else {
            b.tvSpecialty.setVisibility(View.GONE);
        }

        // File info row
        b.layoutFileInfo.setVisibility(View.VISIBLE);
        b.tvFileType.setText(report.fileType != null ? report.fileType.toUpperCase() : "FILE");
        String fileSizeStr = "Unknown size";
        if (report.fileSize != null) {
            try {
                long bytes = Long.parseLong(report.fileSize);
                fileSizeStr = bytes > 1024 * 1024 ?
                    String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0)) :
                    String.format(Locale.US, "%.0f KB", bytes / 1024.0);
            } catch (Exception e) { fileSizeStr = report.fileSize; }
        }
        b.tvFileSize.setText(fileSizeStr);

        if (report.analyzedAt != null) {
            b.tvAnalyzedAt.setVisibility(View.VISIBLE);
            b.tvAnalyzedAt.setText("Analyzed: " + formatDate(report.analyzedAt));
        }

        // ── AI ANALYSIS SECTIONS ──
        JsonObject ai = report.aiAnalysis;
        
        // Reset all AI cards
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
            
            // ── 2. AI SUMMARY ──
            String summary = report.getAiSummary();
            if (summary != null && !summary.isEmpty()) {
                b.cardSummary.setVisibility(View.VISIBLE);
                b.tvSummary.setText(summary);
                String model = report.getModelUsed();
                if (model != null) {
                    b.tvModelUsed.setVisibility(View.VISIBLE);
                    b.tvModelUsed.setText("Model: " + model);
                }
            }

            // ── 3. EXTRACTION INFO ──
            JsonObject extraction = smartParseObj(ai.get("extraction_info"));
            if (extraction != null) {
                b.cardExtraction.setVisibility(View.VISIBLE);
                b.layoutExtractionBadges.removeAllViews();
                
                String ft = safeStr(extraction, "file_type", null);
                if (ft != null) addExtractionBadge("📄 " + ft.toUpperCase(), "#F1F5F9", "@color/text_secondary");

                String tl = safeStr(extraction, "text_length", null);
                if (tl != null) addExtractionBadge(tl + " chars", "#F1F5F9", "@color/text_secondary");

                boolean success = false;
                try { success = extraction.get("extraction_successful").getAsBoolean(); } catch (Exception e) {}
                addExtractionBadge(success ? "✅ Extracted" : "❌ Failed",
                    success ? "#F0FDF4" : "#FEF2F2", success ? "@color/badge_text_green" : "@color/badge_text_red");

                String ocrScore = safeStr(extraction, "ocr_quality_score", null);
                if (ocrScore != null) {
                    try {
                        int pct = (int)(Double.parseDouble(ocrScore) * 100);
                        addExtractionBadge("OCR " + pct + "%",
                            pct >= 80 ? "#F0FDF4" : pct >= 50 ? "#FFFBEB" : "#FEF2F2",
                            pct >= 80 ? "@color/badge_text_green" : pct >= 50 ? "@color/badge_text_amber" : "@color/badge_text_red");
                    } catch (Exception e) {}
                }

                String preview = safeStr(extraction, "extracted_text_preview", null);
                if (preview != null && !preview.isEmpty()) {
                    b.tvExtractionPreview.setVisibility(View.VISIBLE);
                    b.tvExtractionPreview.setText(preview);
                }
            }

            // ── 4. PATIENT INFO ──
            JsonObject pi = smartParseObj(ai.get("patient_info"));
            if (pi != null && pi.entrySet().size() > 0) {
                b.cardPatientInfo.setVisibility(View.VISIBLE);
                b.layoutPatientGrid.removeAllViews();
                if (canEdit) injectEditButton(b.cardPatientInfo, "patient_info");
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

            // ── 5. DIAGNOSES ──
            JsonArray diagnoses = smartParseArray(ai.get("diagnoses"));
            if (diagnoses != null) {
                b.cardDiagnoses.setVisibility(View.VISIBLE);
                if (canEdit) injectEditButton(b.cardDiagnoses, "diagnoses");
                b.tvDiagnosesCount.setText(diagnoses.size() + " identified");
                if (diagnoses.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement e : diagnoses) {
                        try { sb.append("• ").append(e.getAsString()).append("\n"); }
                        catch (Exception ex) { sb.append("• ").append(e.toString()).append("\n"); }
                    }
                    b.tvDiagnoses.setText(sb.toString().trim());
                } else {
                    b.tvDiagnoses.setText("No diagnoses identified in this report.");
                    b.tvDiagnoses.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                }
            }

            // ── 6. SYMPTOMS ──
            JsonArray symptoms = smartParseArray(ai.get("symptoms"));
            if (symptoms != null) {
                b.cardSymptoms.setVisibility(View.VISIBLE);
                if (canEdit) injectEditButton(b.cardSymptoms, "symptoms");
                if (symptoms.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement e : symptoms) {
                        try { sb.append("• ").append(e.getAsString()).append("\n"); }
                        catch (Exception ex) { sb.append("• ").append(e.toString()).append("\n"); }
                    }
                    b.tvSymptoms.setText(sb.toString().trim());
                } else {
                    b.tvSymptoms.setText("No symptoms reported.");
                    b.tvSymptoms.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                }
            }

            // ── 7. VITAL SIGNS ──
            JsonObject vitals = smartParseObj(ai.get("vital_signs"));
            if (vitals != null) {
                b.gridVitals.removeAllViews();
                if (canEdit) injectEditButton(b.cardVitals, "vital_signs");
                boolean hasAnyVital = false;
                for (Map.Entry<String, JsonElement> entry : vitals.entrySet()) {
                    String val = "";
                    try { val = entry.getValue().isJsonNull() ? "" : entry.getValue().getAsString(); } catch (Exception ex) { val = ""; }
                    if (val.isEmpty() || val.equals("null")) continue;
                    hasAnyVital = true;
                    addVitalItem(b.gridVitals, formatKey(entry.getKey()), val);
                }
                b.cardVitals.setVisibility(hasAnyVital ? View.VISIBLE : View.GONE);
            }

            // ── 8. LAB RESULTS ──
            JsonArray labs = smartParseArray(ai.get("lab_results"));
            if (labs != null) {
                b.cardLabResults.setVisibility(View.VISIBLE);
                if (canEdit) injectEditButton(b.cardLabResults, "lab_results");
                b.tvLabCount.setText(labs.size() + " tests analyzed");
                b.layoutLabItems.removeAllViews();
                if (labs.size() > 0) {
                    for (JsonElement e : labs) {
                        if (e.isJsonObject()) addLabItem(b.layoutLabItems, e.getAsJsonObject());
                    }
                } else {
                    TextView tv = new TextView(this);
                    tv.setText("No lab tests found in this report.");
                    tv.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                    tv.setTextSize(13);
                    b.layoutLabItems.addView(tv);
                }
            }

            // ── 9. MEDICATIONS ──
            JsonArray meds = smartParseArray(ai.get("current_medications"));
            if (meds != null) {
                b.cardMedications.setVisibility(View.VISIBLE);
                if (canEdit) injectEditButton(b.cardMedications, "medications");
                if (meds.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement e : meds) {
                        if (e.isJsonObject()) {
                            JsonObject obj = e.getAsJsonObject();
                            String mName = safeStr(obj, "name", safeStr(obj, "medication", "Unknown"));
                            String mDose = safeStr(obj, "dosage", safeStr(obj, "dose", ""));
                            String freq  = safeStr(obj, "frequency", "");
                            sb.append("• ").append(mName);
                            if (mDose != null && !mDose.isEmpty()) sb.append("  —  ").append(mDose);
                            if (freq != null && !freq.isEmpty()) sb.append("  (").append(freq).append(")");
                            sb.append("\n");
                        } else {
                            try { sb.append("• ").append(e.getAsString()).append("\n"); } catch (Exception ex) {}
                        }
                    }
                    b.tvMedications.setText(sb.toString().trim());
                } else {
                    b.tvMedications.setText("No medications identified.");
                    b.tvMedications.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
                }
            }

            // ── 10. ABNORMAL FINDINGS ──
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
                        if (significance != null && !significance.isEmpty()) sb.append("  [").append(significance).append("]");
                        sb.append("\n");
                    } else {
                        try { sb.append("⚠ ").append(e.getAsString()).append("\n"); } catch (Exception ex) {}
                    }
                }
                b.tvAbnormal.setText(sb.toString().trim());
            }

            // ── 11. AI CLINICAL SUGGESTIONS ──
            JsonArray suggestions = smartParseArray(ai.get("clinical_suggestions"));
            if (suggestions != null && suggestions.size() > 0) {
                b.cardSuggestions.setVisibility(View.VISIBLE);
                b.layoutSuggestionItems.removeAllViews();
                for (int i = 0; i < suggestions.size(); i++) {
                    JsonElement e = suggestions.get(i);
                    if (e.isJsonObject()) {
                        addSuggestionItem(b.layoutSuggestionItems, e.getAsJsonObject(), i);
                    } else {
                        try { addSimpleText(b.layoutSuggestionItems, (i+1) + ". " + e.getAsString()); } catch (Exception ex) {}
                    }
                }
            }
        }

        // ── 12. SUGGESTED DOCTORS ──
        if (report.suggestedDoctors != null && !report.suggestedDoctors.isEmpty() && isPatient) {
            b.cardSuggestedDoctors.setVisibility(View.VISIBLE);
            b.layoutDoctorItems.removeAllViews();
            for (int i = 0; i < report.suggestedDoctors.size(); i++) {
                addDoctorItem(b.layoutDoctorItems, report.suggestedDoctors.get(i), i);
            }
        }

        // ── 13. DOCTOR NOTES ──
        if (report.doctorNotes != null && !report.doctorNotes.isEmpty()) {
            b.cardDoctorNotes.setVisibility(View.VISIBLE);
            b.tvDoctorNotes.setText(report.doctorNotes);
        } else {
            b.cardDoctorNotes.setVisibility(View.GONE);
        }

        // ── ACTIONS ──
        b.btnChat.setVisibility(report.assignedDoctorId != null ? View.VISIBLE : View.GONE);
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
            // Show Ask AI button if report is analyzed (has extracted text)
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

        b.switchDoctorEdit.setChecked(report.doctorEditPermission);
        b.switchDoctorEdit.setOnCheckedChangeListener((sw, checked) -> {
            if (!sw.isPressed()) return;
            Map<String, Boolean> body = new HashMap<>();
            body.put("allow", checked);
            ApiClient.get().setDoctorEditPermission(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
                @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                    Toast.makeText(ReportDetailActivity.this, checked ? "Doctor can now edit" : "Edit access revoked", Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {}
            });
        });

        b.cardReview.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
        b.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    // ══════════════════════════════════════════════════════════════════
    // EDIT FUNCTIONALITY (React Parity)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Injects a small "Edit" button into the first LinearLayout child of a card.
     * The button opens the EditSectionDialog for the given section key.
     */
    private void injectEditButton(com.google.android.material.card.MaterialCardView card, String sectionKey) {
        // Find the header LinearLayout (first horizontal LL with gravity center_vertical)
        LinearLayout cardRoot = null;
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof LinearLayout) { cardRoot = (LinearLayout) child; break; }
        }
        if (cardRoot == null) return;

        // Find first horizontal LinearLayout (header row)
        LinearLayout headerRow = null;
        for (int i = 0; i < cardRoot.getChildCount(); i++) {
            View child = cardRoot.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) child;
                if (ll.getOrientation() == LinearLayout.HORIZONTAL) { headerRow = ll; break; }
            }
        }
        if (headerRow == null) return;

        // Check if we've already injected (tag-based)
        if (headerRow.getTag() != null && headerRow.getTag().equals("edit_injected")) return;
        headerRow.setTag("edit_injected");

        // Build small edit button
        TextView btnEdit = new TextView(this);
        btnEdit.setText("Edit");
        btnEdit.setTextSize(12);
        btnEdit.setTypeface(null, Typeface.BOLD);
        btnEdit.setTextColor(ContextCompat.getColor(this, R.color.primary));
        btnEdit.setBackgroundResource(R.drawable.bg_badge);
        btnEdit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.badge_bg_blue)));
        int hp = dpToPx(12); int vp = dpToPx(6);
        btnEdit.setPadding(hp, vp, hp, vp);
        btnEdit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0);
        btnEdit.setCompoundDrawablePadding(dpToPx(4));
        btnEdit.setCompoundDrawableTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        btnEdit.setOnClickListener(v -> openEditDialog(sectionKey));
        headerRow.addView(btnEdit);
    }

    private void openEditDialog(String sectionKey) {
        if (report == null || report.aiAnalysis == null) return;
        new EditSectionDialog(this, sectionKey, report.aiAnalysis, (section, updatedAnalysis) -> {
            saveEditedAnalysis(updatedAnalysis);
        }).show();
    }

    private void saveEditedAnalysis(JsonObject updatedAnalysis) {
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();
        
        // Convert JsonObject to Map for Retrofit
        Map<String, Object> body = new HashMap<>();
        body.put("ai_analysis", new com.google.gson.Gson().fromJson(updatedAnalysis, Map.class));
        
        ApiClient.get().updateAiAnalysis(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    bindReport();
                    Toast.makeText(ReportDetailActivity.this, "Analysis updated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReportDetailActivity.this, "Failed to save changes", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                Log.e(TAG, "Save edit failed", t);
                Toast.makeText(ReportDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    // DYNAMIC VIEW BUILDERS
    // ══════════════════════════════════════════════════════════════════


    private void addPatientField(LinearLayout container, String label, String value, String color) {
        if (value == null || value.isEmpty() || value.equals("null")) return;
        
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_badge);
        int bgColor;
        int txtColor;
        switch (color) {
            case "green": bgColor = ContextCompat.getColor(this, R.color.badge_bg_green); txtColor = ContextCompat.getColor(this, R.color.badge_text_green); break;
            case "red":   bgColor = ContextCompat.getColor(this, R.color.badge_bg_red);   txtColor = ContextCompat.getColor(this, R.color.badge_text_red);   break;
            default:      bgColor = ContextCompat.getColor(this, R.color.badge_bg_blue);  txtColor = ContextCompat.getColor(this, R.color.badge_text_blue);  break;
        }
        row.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        int pad = dpToPx(12);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
            case "@color/badge_text_green": txtColor = ContextCompat.getColor(this, R.color.badge_text_green); break;
            case "@color/badge_text_red":   txtColor = ContextCompat.getColor(this, R.color.badge_text_red); break;
            case "@color/badge_text_amber": txtColor = ContextCompat.getColor(this, R.color.badge_text_amber); break;
            default: txtColor = ContextCompat.getColor(this, R.color.text_secondary); break;
        }
        badge.setTextColor(txtColor);
        badge.setTypeface(null, Typeface.BOLD);
        int hp = dpToPx(8); int vp = dpToPx(4);
        badge.setPadding(hp, vp, hp, vp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
            boolean isAbn = labStatus.equalsIgnoreCase("abnormal") || labStatus.equalsIgnoreCase("high") || labStatus.equalsIgnoreCase("low") || labStatus.equalsIgnoreCase("critical");
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(10);
        item.setLayoutParams(lp);
        
        // Priority + Title
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
            case "CRITICAL": applyBadgeColor(tvPri, "red"); break;
            case "HIGH":     applyBadgeColor(tvPri, "orange"); break;
            case "MEDIUM":   applyBadgeColor(tvPri, "amber"); break;
            default:         applyBadgeColor(tvPri, "green"); break;
        }
        header.addView(tvPri);
        
        String title = safeStr(s, "suggestion", safeStr(s, "title", safeStr(s, "recommendation", "Suggestion " + (index + 1))));
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
        
        // Category
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
        
        // Reasoning
        String reason = safeStr(s, "reasoning", safeStr(s, "reason", safeStr(s, "rationale", null)));
        if (reason != null && !reason.isEmpty()) {
            TextView tvReason = new TextView(this);
            tvReason.setText(reason);
            tvReason.setTextSize(13);
            tvReason.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvReason.setPadding(0, dpToPx(4), 0, 0);
            item.addView(tvReason);
        }
        
        // Confidence
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
            } catch (Exception ex) {}
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(8);
        item.setLayoutParams(lp);

        // Rank circle
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

        int score = (int)(doc.matchScore * 100);
        TextView tvScore = new TextView(this);
        tvScore.setText("Match: " + score + "%");
        tvScore.setTextSize(12);
        tvScore.setTypeface(null, Typeface.BOLD);
        tvScore.setTextColor(ContextCompat.getColor(this, R.color.badge_text_green));
        info.addView(tvScore);

        if (doc.specializations != null && !doc.specializations.isEmpty()) {
            TextView tvSpecs = new TextView(this);
            StringBuilder sb = new StringBuilder();
            for (String spec : doc.specializations) sb.append(spec).append(", ");
            tvSpecs.setText(sb.toString().replaceAll(", $", ""));
            tvSpecs.setTextSize(11);
            tvSpecs.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            info.addView(tvSpecs);
        }
        item.addView(info);

        // Assign button (only if no doctor assigned yet)
        if (report.assignedDoctorId == null) {
            com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(this);
            btn.setText("Assign");
            btn.setTextSize(11);
            btn.setAllCaps(false);
            btn.setCornerRadius(dpToPx(10));
            btn.setMinimumHeight(dpToPx(36));
            btn.setMinHeight(dpToPx(36));
            btn.setPadding(dpToPx(12), 0, dpToPx(12), 0);
            btn.setOnClickListener(v -> assignDoctor(doc.doctorId, doc.doctorName));
            item.addView(btn);
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

    // ══════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════

    private void triggerAnalysis() {
        b.btnAnalyze.setEnabled(false);
        b.btnAnalyze.setText("Starting...");
        ApiClient.get().analyzeReport(reportId).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().report != null) {
                    report = r.body().report;
                    bindReport();
                    Toast.makeText(ReportDetailActivity.this, "AI Analysis started!", Toast.LENGTH_SHORT).show();
                    startPolling();
                } else {
                    b.btnAnalyze.setEnabled(true);
                    b.btnAnalyze.setText("Analyze with AI");
                    Toast.makeText(ReportDetailActivity.this, "Failed to start", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                b.btnAnalyze.setEnabled(true);
                b.btnAnalyze.setText("Analyze with AI");
            }
        });
    }

    private void assignDoctor(String doctorId, String doctorName) {
        Map<String, String> body = new HashMap<>();
        body.put("doctor_id", doctorId);
        ApiClient.get().assignDoctor(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(ReportDetailActivity.this, "Dr. " + doctorName + " assigned!", Toast.LENGTH_SHORT).show();
                    loadReport();
                }
            }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                Toast.makeText(ReportDetailActivity.this, "Assignment failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReview() {
        String notes = b.etReviewNotes.getText().toString().trim();
        Map<String, String> body = new HashMap<>();
        body.put("notes", notes);
        ApiClient.get().reviewReport(reportId, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(ReportDetailActivity.this, "Review saved!", Toast.LENGTH_SHORT).show();
                    loadReport();
                }
            }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {}
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this).setTitle("Delete Report")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete", (d, w) -> {
                ApiClient.get().deleteReport(reportId).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                        Toast.makeText(ReportDetailActivity.this, "Report deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
                });
            }).setNegativeButton("Cancel", null).show();
    }

    // ══════════════════════════════════════════════════════════════════
    // UTILITIES
    // ══════════════════════════════════════════════════════════════════

    private void applyBadgeColor(TextView tv, String color) {
        int bgRes, txtRes;
        switch (color) {
            case "red":    bgRes = R.color.badge_bg_red;    txtRes = R.color.badge_text_red;    break;
            case "orange": bgRes = R.color.badge_bg_orange; txtRes = R.color.badge_text_orange; break;
            case "amber":  bgRes = R.color.badge_bg_amber;  txtRes = R.color.badge_text_amber;  break;
            case "green":  bgRes = R.color.badge_bg_green;  txtRes = R.color.badge_text_green;  break;
            default:       bgRes = R.color.badge_bg_blue;   txtRes = R.color.badge_text_blue;   break;
        }
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, bgRes)));
        tv.setTextColor(ContextCompat.getColor(this, txtRes));
    }

    private String formatKey(String key) {
        String s = key.replace("_", " ");
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String formatDate(String iso) {
        if (iso == null) return "";
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = sdfIn.parse(iso);
            if (d != null) return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) {}
        return iso;
    }

    private int dpToPx(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    private JsonObject smartParseObj(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonObject()) return el.getAsJsonObject();
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            try {
                String str = el.getAsString().trim().replace("'", "\"").replace("None", "null").replace("False", "false").replace("True", "true");
                return new JsonParser().parse(str).getAsJsonObject();
            } catch (Exception e) { Log.e(TAG, "Obj parse fail", e); }
        }
        return null;
    }

    private JsonArray smartParseArray(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonArray()) return el.getAsJsonArray();
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            try {
                String str = el.getAsString().trim().replace("'", "\"");
                return new JsonParser().parse(str).getAsJsonArray();
            } catch (Exception e) { Log.e(TAG, "Array parse fail", e); }
        }
        return null;
    }

    private String safeStr(JsonObject obj, String key, String fallback) {
        if (obj == null) return fallback;
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsString();
        } catch (Exception e) {}
        return fallback;
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
