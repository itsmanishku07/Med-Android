package com.medreport.ai.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.medreport.ai.R;
import com.medreport.ai.databinding.ItemDashboardReportBinding;
import com.medreport.ai.models.ReportModel;
import com.medreport.ai.utils.AuthManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportDashboardAdapter extends RecyclerView.Adapter<ReportDashboardAdapter.Holder> {

    private final List<ReportModel> list;
    private final Context ctx;
    private final boolean isDoctor;
    private final OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onConsultDoctor(ReportModel report);
        void onDelete(ReportModel report);
        void onAcceptCase(ReportModel report);
        void onChatPatient(ReportModel report);
        void onToggleArchive(ReportModel report);
    }

    public ReportDashboardAdapter(Context ctx, List<ReportModel> list, boolean isDoctor, OnItemInteractionListener listener) {
        this.ctx = ctx;
        this.list = list;
        this.isDoctor = isDoctor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemDashboardReportBinding.inflate(LayoutInflater.from(ctx), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ReportModel r = list.get(position);
        
        h.b.tvFileName.setText(r.fileName != null ? r.fileName : "Unknown Report");
        
        if (r.uploadedAt != null) {
            try {
                SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date d = sdfIn.parse(r.uploadedAt);
                if (d != null) {
                    SimpleDateFormat sdfOut = new SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault());
                    h.b.tvTimestamp.setText("Uploaded: " + sdfOut.format(d));
                }
            } catch (Exception e) {
                h.b.tvTimestamp.setText("Uploaded: " + r.uploadedAt);
            }
        }

        String status = r.status != null ? r.status : "PENDING";
        h.b.tvSeverityBadge.setVisibility(View.GONE);
        h.b.ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.icon_tint));
        
        if (status.equals("PENDING")) {
            h.b.ivStatusIcon.setImageResource(R.drawable.ic_clock);
        } else if (status.equals("ANALYZING")) {
            h.b.ivStatusIcon.setImageResource(R.drawable.ic_clock);
            h.b.ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.primary));
        } else if (status.equals("ANALYZED") || status.equals("REVIEWED")) {
            h.b.ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
            h.b.ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.badge_text_green));
        }

        h.b.layoutAiSummary.setVisibility(View.GONE);
        h.b.layoutAbnormalAlert.setVisibility(View.GONE);
        h.b.layoutDoctorAssigned.setVisibility(View.GONE);

        if (r.aiAnalysis != null && r.aiAnalysis.isJsonObject() && !status.equals("ANALYZING") && !status.equals("PENDING")) {
            h.b.layoutAiSummary.setVisibility(View.VISIBLE);
            com.google.gson.JsonObject aiObj = r.aiAnalysis.getAsJsonObject();
            
            if (aiObj.has("diagnoses") && aiObj.get("diagnoses").isJsonArray()) {
                com.google.gson.JsonArray diagObj = aiObj.get("diagnoses").getAsJsonArray();
                if (diagObj.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < diagObj.size(); i++) {
                        String displayDiag = com.medreport.ai.utils.MedicalDataUtils.getDisplayString(diagObj.get(i));
                        sb.append(displayDiag);
                        if (i < diagObj.size() - 1) sb.append(", ");
                    }
                    h.b.tvDiagnosesPreview.setText("Diagnoses: " + sb.toString());
                } else h.b.tvDiagnosesPreview.setVisibility(View.GONE);
            } else {
                h.b.tvDiagnosesPreview.setVisibility(View.GONE);
            }

            if (aiObj.has("abnormal_findings") && aiObj.get("abnormal_findings").isJsonArray()) {
                com.google.gson.JsonArray abnormalObj = aiObj.get("abnormal_findings").getAsJsonArray();
                if (abnormalObj.size() > 0) {
                    h.b.layoutAbnormalAlert.setVisibility(View.VISIBLE);
                    h.b.tvAbnormalText.setText(abnormalObj.size() + " Abnormal finding(s) detected");
                }
            }

            String sev = r.getSeverityLevel();
            if (sev != null) {
                h.b.tvSeverityBadge.setVisibility(View.VISIBLE);
                h.b.tvSeverityBadge.setText(sev);
                
                int bgRes = R.color.badge_bg_blue;
                int txtRes = R.color.badge_text_blue;
                
                switch (sev) {
                    case "CRITICAL": bgRes = R.color.badge_bg_red; txtRes = R.color.badge_text_red; break;
                    case "HIGH": bgRes = R.color.badge_bg_orange; txtRes = R.color.badge_text_orange; break;
                    case "MEDIUM": bgRes = R.color.badge_bg_amber; txtRes = R.color.badge_text_amber; break;
                    case "LOW": bgRes = R.color.badge_bg_green; txtRes = R.color.badge_text_green; break;
                }
                h.b.tvSeverityBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, bgRes)));
                h.b.tvSeverityBadge.setTextColor(ContextCompat.getColor(ctx, txtRes));
            }
        }

        if (r.assignedDoctorId != null) {
            h.b.layoutDoctorAssigned.setVisibility(View.VISIBLE);
            if (r.assignedDoctorName != null) {
                h.b.tvDoctorAssignedName.setText("Shared with Dr. " + r.assignedDoctorName);
                h.b.tvDoctorAssignedName.setPaintFlags(h.b.tvDoctorAssignedName.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                h.b.tvDoctorAssignedName.setOnClickListener(v -> {
                    Intent intent = new Intent(ctx, DoctorProfileActivity.class);
                    intent.putExtra(DoctorProfileActivity.EXTRA_DOCTOR_ID, r.assignedDoctorId);
                    ctx.startActivity(intent);
                });
            } else {
                h.b.tvDoctorAssignedName.setText("Doctor assigned");
            }
        }

        h.b.btnConsultDoctor.setVisibility(View.GONE);
        h.b.btnDelete.setVisibility(View.GONE);
        h.b.btnAcceptCase.setVisibility(View.GONE);
        h.b.btnChatPatient.setVisibility(View.GONE);
        h.b.btnArchiveToggle.setVisibility(View.GONE);
        h.b.tvPatientName.setVisibility(View.GONE);

        if (isDoctor) {
            h.b.tvPatientName.setVisibility(View.VISIBLE);
            h.b.tvPatientName.setText("Patient: " + (r.patientId != null ? r.patientId : "Unknown"));
            
            if (status.equals("PENDING")) {
                h.b.btnAcceptCase.setVisibility(View.VISIBLE);
            } else {
                h.b.btnChatPatient.setVisibility(View.VISIBLE);
            }
            h.b.btnArchiveToggle.setVisibility(View.VISIBLE);
        } else {
            h.b.btnDelete.setVisibility(View.VISIBLE);
            if (r.assignedDoctorId != null) {
                h.b.btnConsultDoctor.setVisibility(View.VISIBLE);
            }
        }

        h.b.btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(ctx, ReportDetailActivity.class);
            i.putExtra("report_id", r.id);
            ctx.startActivity(i);
        });
        h.b.btnConsultDoctor.setOnClickListener(v -> listener.onConsultDoctor(r));
        h.b.btnDelete.setOnClickListener(v -> listener.onDelete(r));
        h.b.btnAcceptCase.setOnClickListener(v -> listener.onAcceptCase(r));
        h.b.btnChatPatient.setOnClickListener(v -> listener.onChatPatient(r));
        h.b.btnArchiveToggle.setOnClickListener(v -> listener.onToggleArchive(r));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class Holder extends RecyclerView.ViewHolder {
        ItemDashboardReportBinding b;
        public Holder(ItemDashboardReportBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
