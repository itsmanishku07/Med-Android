package com.medreport.ai.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.medreport.ai.R;
import com.medreport.ai.databinding.ItemAppointmentBinding;
import com.medreport.ai.models.AppointmentModel;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private final Context context;
    private final List<AppointmentModel> appointmentList;
    private final boolean isDoctorView;
    private final OnAppointmentInteractionListener listener;

    public interface OnAppointmentInteractionListener {
        void onManage(AppointmentModel appointment);
        void onItemClicked(AppointmentModel appointment);
    }

    public AppointmentAdapter(Context context, List<AppointmentModel> appointmentList, boolean isDoctorView, OnAppointmentInteractionListener listener) {
        this.context = context;
        this.appointmentList = appointmentList;
        this.isDoctorView = isDoctorView;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppointmentBinding binding = ItemAppointmentBinding.inflate(LayoutInflater.from(context), parent, false);
        return new AppointmentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        AppointmentModel appointment = appointmentList.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppointmentBinding b;

        public AppointmentViewHolder(ItemAppointmentBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        public void bind(AppointmentModel appt) {
            if (isDoctorView) {
                b.tvUserName.setText("Patient: " + (appt.patientName != null ? appt.patientName : "User"));
                b.btnAction.setVisibility(appt.isPending() ? View.VISIBLE : View.GONE);
                b.btnAction.setText("Schedule");
            } else {
                b.tvUserName.setText("Dr. " + (appt.doctorName != null ? appt.doctorName : "Doctor"));
                b.btnAction.setVisibility(View.GONE);
            }

            b.tvStatusBadge.setText(appt.status);
            setStatusBadgeStyle(appt.status);

            if (appt.scheduledAt != null && !appt.scheduledAt.isEmpty()) {
                b.layoutScheduledTime.setVisibility(View.VISIBLE);
                b.tvScheduledTime.setText(formatDateTime(appt.scheduledAt));
            } else {
                b.layoutScheduledTime.setVisibility(View.GONE);
            }

            if (appt.preferredTime != null && !appt.preferredTime.isEmpty()) {
                b.layoutPreferredTime.setVisibility(View.VISIBLE);
                b.tvPreferredTime.setText("Preferred: " + formatDateTime(appt.preferredTime));
            } else {
                b.layoutPreferredTime.setVisibility(View.GONE);
            }

            if (appt.notes != null && !appt.notes.isEmpty()) {
                b.tvNotes.setVisibility(View.VISIBLE);
                b.tvNotes.setText(appt.notes);
            } else {
                b.tvNotes.setVisibility(View.GONE);
            }

            if (appt.doctorNotes != null && !appt.doctorNotes.isEmpty()) {
                b.tvDoctorNotes.setVisibility(View.VISIBLE);
                b.tvDoctorNotes.setText("Doctor's Note: " + appt.doctorNotes);
            } else {
                b.tvDoctorNotes.setVisibility(View.GONE);
            }

            b.btnAction.setOnClickListener(v -> listener.onManage(appt));
            itemView.setOnClickListener(v -> listener.onItemClicked(appt));
        }

        private void setStatusBadgeStyle(String status) {
            int bgColor, textColor;
            switch (status) {
                case "ACCEPTED":
                    bgColor = R.color.badge_bg_green;
                    textColor = R.color.badge_text_green;
                    break;
                case "REJECTED":
                case "CANCELLED":
                    bgColor = R.color.badge_bg_red;
                    textColor = R.color.badge_text_red;
                    break;
                case "COMPLETED":
                    bgColor = R.color.badge_bg_blue;
                    textColor = R.color.badge_text_blue;
                    break;
                default: // PENDING
                    bgColor = R.color.badge_bg_amber;
                    textColor = R.color.badge_text_amber;
                    break;
            }
            b.tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, bgColor));
            b.tvStatusBadge.setTextColor(ContextCompat.getColor(context, textColor));
        }

        private String formatDateTime(String isoString) {
            try {
                // Simple formatting for display (In a real app, use SimpleDateFormat or Java 8 Time)
                return isoString.replace("T", " ").substring(0, 16);
            } catch (Exception e) {
                return isoString;
            }
        }
    }
}
