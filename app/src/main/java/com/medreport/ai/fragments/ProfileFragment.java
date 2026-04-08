package com.medreport.ai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.medreport.ai.activities.LoginActivity;
import com.medreport.ai.databinding.FragmentProfileBinding;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.utils.AuthManager;
import com.medreport.ai.R;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;
    private UserModel user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        user = AuthManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        setupUI();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        user = AuthManager.getInstance().getCurrentUser();
        setupUI();
    }

    private void setupUI() {
        b.tvName.setText(user.name != null ? user.name : "Unknown User");
        b.tvEmail.setText(user.email);
        b.tvInitials.setText(user.getInitial());
        b.tvPhone.setText(user.phone != null && !user.phone.isEmpty() ? user.phone : "Not set");
        b.chipRole.setText(user.role);

        if (user.profilePicture != null && user.profilePicture.startsWith("data:image")) {
            try {
                String pureBase64 = user.profilePicture.split(",")[1];
                byte[] decodedString = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                b.ivProfilePic.setImageBitmap(bitmap);
                b.ivProfilePic.setVisibility(View.VISIBLE);
                b.tvInitials.setVisibility(View.GONE);
            } catch (Exception e) {
                b.ivProfilePic.setVisibility(View.GONE);
                b.tvInitials.setVisibility(View.VISIBLE);
            }
        } else {
            b.ivProfilePic.setVisibility(View.GONE);
            b.tvInitials.setVisibility(View.VISIBLE);
        }

        if (user.isDoctor() || user.isAdmin()) {
            b.layoutDoctorOnly.setVisibility(View.VISIBLE);
            b.layoutPatientServices.setVisibility(View.GONE);
            if (user.specializations != null && !user.specializations.isEmpty()) {
                b.tvSpecializations.setText(String.join(", ", user.specializations));
            } else {
                b.tvSpecializations.setText("No specializations added");
            }
        } else {
            b.layoutDoctorOnly.setVisibility(View.GONE);
            b.layoutPatientServices.setVisibility(View.VISIBLE);
        }

        // Show admin section for admins
        if (user.isAdmin()) {
            b.layoutAdminOnly.setVisibility(View.VISIBLE);
        } else {
            b.layoutAdminOnly.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        b.btnLogout.setOnClickListener(v -> handleLogout());
        b.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.medreport.ai.activities.ProfileActivity.class);
            startActivity(intent);
        });

        b.btnFindDoctors.setOnClickListener(v -> replaceFragment(new DoctorListingFragment()));
        b.btnMyAppointments.setOnClickListener(v -> replaceFragment(new MyAppointmentsFragment()));
        b.btnManageAppointments.setOnClickListener(v -> replaceFragment(new DoctorAppointmentsFragment()));
        b.btnManageAvailability.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.medreport.ai.activities.DoctorAvailabilityActivity.class);
            startActivity(intent);
        });
        b.btnWaterReminder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.medreport.ai.activities.WaterReminderSettingsActivity.class);
            startActivity(intent);
        });
        
        // Admin dashboard button
        if (b.btnAdminDashboard != null) {
            b.btnAdminDashboard.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), com.medreport.ai.activities.AdminDashboardActivity.class);
                startActivity(intent);
            });
        }
    }

    private void replaceFragment(Fragment f) {
        getParentFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentContainer, f)
            .addToBackStack(null)
            .commit();
    }

    private void handleLogout() {
        AuthManager.getInstance().signOut();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
