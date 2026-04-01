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
            // Unlikely if they are on this screen, but handle it
            return;
        }

        setupUI();
        setupListeners();
    }

    private void setupUI() {
        b.tvName.setText(user.name != null ? user.name : "Unknown User");
        b.tvEmail.setText(user.email);
        b.tvInitials.setText(user.getInitial());
        b.tvPhone.setText(user.phone != null && !user.phone.isEmpty() ? user.phone : "Not set");
        b.chipRole.setText(user.role);

        if (user.isDoctor() || user.isAdmin()) {
            b.layoutDoctorOnly.setVisibility(View.VISIBLE);
            if (user.specializations != null && !user.specializations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < user.specializations.size(); i++) {
                    sb.append(user.specializations.get(i));
                    if (i < user.specializations.size() - 1) sb.append(", ");
                }
                b.tvSpecializations.setText(sb.toString());
            } else {
                b.tvSpecializations.setText("No specializations added");
            }
        }
    }

    private void setupListeners() {
        b.btnLogout.setOnClickListener(v -> handleLogout());
        b.btnEdit.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit profile coming soon", Toast.LENGTH_SHORT).show();
        });
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
