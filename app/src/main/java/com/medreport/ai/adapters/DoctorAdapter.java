package com.medreport.ai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medreport.ai.R;
import com.medreport.ai.databinding.ItemDoctorBinding;
import com.medreport.ai.models.UserModel;
import com.bumptech.glide.Glide;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private final Context context;
    private final List<UserModel> doctorList;
    private final OnDoctorInteractionListener listener;

    public interface OnDoctorInteractionListener {
        void onBook(UserModel doctor);
        void onDetails(UserModel doctor);
    }

    public DoctorAdapter(Context context, List<UserModel> doctorList, OnDoctorInteractionListener listener) {
        this.context = context;
        this.doctorList = doctorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDoctorBinding binding = ItemDoctorBinding.inflate(LayoutInflater.from(context), parent, false);
        return new DoctorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        UserModel doctor = doctorList.get(position);
        holder.bind(doctor);
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }

    class DoctorViewHolder extends RecyclerView.ViewHolder {
        private final ItemDoctorBinding b;

        public DoctorViewHolder(ItemDoctorBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        public void bind(UserModel doctor) {
            b.tvDoctorName.setText("Dr. " + doctor.name);
            
            if (doctor.specializations != null && !doctor.specializations.isEmpty()) {
                b.tvSpecializations.setText(String.join(", ", doctor.specializations));
            } else {
                b.tvSpecializations.setText("General Physician");
            }

            if (doctor.profile != null) {
                if (doctor.profile.experience != null) {
                    b.tvExperience.setText("• " + doctor.profile.experience + " Years Exp.");
                } else {
                    b.tvExperience.setText("");
                }

                if (doctor.profile.availability != null) {
                    b.tvAvailability.setText(doctor.profile.availability);
                } else {
                    b.tvAvailability.setText("Consult for timing");
                }
            }
            
            // Display rating
            if (doctor.averageRating != null && doctor.averageRating > 0) {
                b.ratingBar.setRating(doctor.averageRating.floatValue());
                b.ratingBar.setVisibility(android.view.View.VISIBLE);
                if (doctor.totalReviews != null && doctor.totalReviews > 0) {
                    b.tvRating.setText(String.format(java.util.Locale.US, "%.1f (%d)", 
                        doctor.averageRating, doctor.totalReviews));
                    b.tvRating.setVisibility(android.view.View.VISIBLE);
                } else {
                    b.tvRating.setVisibility(android.view.View.GONE);
                }
            } else {
                b.ratingBar.setVisibility(android.view.View.GONE);
                b.tvRating.setVisibility(android.view.View.GONE);
            }

            if (doctor.profilePicture != null && !doctor.profilePicture.isEmpty()) {
                Glide.with(context)
                    .load(doctor.profilePicture)
                    .placeholder(R.drawable.ic_user)
                    .into(b.ivDoctorProfile);
            } else {
                b.ivDoctorProfile.setImageResource(R.drawable.ic_user);
            }

            b.btnBook.setOnClickListener(v -> listener.onBook(doctor));
            itemView.setOnClickListener(v -> listener.onDetails(doctor));
        }
    }
}
