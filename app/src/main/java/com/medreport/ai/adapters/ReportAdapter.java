package com.medreport.ai.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.models.ReportModel;
import com.medreport.ai.utils.DateUtils;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.VH> {
    public interface Listener { void onReportClick(ReportModel r); }
    private final List<ReportModel> items;
    private final Listener listener;

    public ReportAdapter(List<ReportModel> items, Listener listener) { this.items = items; this.listener = listener; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ReportModel r = items.get(pos);
        h.tvName.setText(r.fileName);
        h.tvDate.setText(DateUtils.formatDate(r.uploadedAt));
        h.tvStatus.setText(r.status);
        h.tvSeverity.setText(r.getSeverityLevel());
        int color = severityColor(r.getSeverityLevel());
        h.tvSeverity.setTextColor(color);
        h.tvPrivate.setVisibility(r.isPrivate != null && r.isPrivate ? View.VISIBLE : View.GONE);
        if (r.assignedDoctorName != null) {
            h.tvDoctorName.setVisibility(View.VISIBLE);
            h.tvDoctorName.setText("Shared with Dr. " + r.assignedDoctorName);
            h.tvDoctorName.setPaintFlags(h.tvDoctorName.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            h.tvDoctorName.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(v.getContext(), com.medreport.ai.activities.DoctorProfileActivity.class);
                intent.putExtra(com.medreport.ai.activities.DoctorProfileActivity.EXTRA_DOCTOR_ID, r.assignedDoctorId);
                v.getContext().startActivity(intent);
            });
        } else {
            h.tvDoctorName.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onReportClick(r));
    }

    private int severityColor(String s) {
        switch (s) {
            case "CRITICAL": return Color.parseColor("#DC2626");
            case "HIGH":     return Color.parseColor("#EA580C");
            case "MEDIUM":   return Color.parseColor("#D97706");
            default:         return Color.parseColor("#16A34A");
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvStatus, tvSeverity, tvPrivate, tvDoctorName;
        VH(View v) {
            super(v);
            tvName       = v.findViewById(R.id.tvFileName);
            tvDate       = v.findViewById(R.id.tvDate);
            tvStatus     = v.findViewById(R.id.tvStatus);
            tvSeverity   = v.findViewById(R.id.tvSeverity);
            tvPrivate    = v.findViewById(R.id.tvPrivate);
            tvDoctorName = v.findViewById(R.id.tvDoctorName);
        }
    }
}
