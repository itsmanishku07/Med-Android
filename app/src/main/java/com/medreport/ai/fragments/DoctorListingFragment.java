package com.medreport.ai.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.medreport.ai.R;
import com.medreport.ai.adapters.DoctorAdapter;
import com.medreport.ai.databinding.DialogBookAppointmentBinding;
import com.medreport.ai.databinding.FragmentDoctorListingBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorListingFragment extends Fragment implements DoctorAdapter.OnDoctorInteractionListener {

    private FragmentDoctorListingBinding b;
    private List<UserModel> fullDoctorList = new ArrayList<>();
    private DoctorAdapter adapter;
    private String searchQuery = "";
    private String categoryFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        b = FragmentDoctorListingBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupUI();
        loadDoctors();
    }

    private void setupUI() {
        b.rvDoctors.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DoctorAdapter(requireContext(), new ArrayList<>(), this);
        b.rvDoctors.setAdapter(adapter);

        b.swipeRefresh.setOnRefreshListener(this::loadDoctors);

        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                filterList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.cgFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) categoryFilter = "All";
            else if (checkedId == R.id.chipCardiology) categoryFilter = "Cardiology";
            else if (checkedId == R.id.chipGeneral) categoryFilter = "General";
            else if (checkedId == R.id.chipNeurology) categoryFilter = "Neurology";
            filterList();
        });
    }

    private void loadDoctors() {
        b.progressBar.setVisibility(View.VISIBLE);
        
        // Load doctors and rating stats in parallel
        ApiClient.get().getDoctors().enqueue(new Callback<ResponseModels.DoctorsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.DoctorsResponse> call, Response<ResponseModels.DoctorsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullDoctorList = response.body().doctors;
                    loadRatingStats();
                } else {
                    b.progressBar.setVisibility(View.GONE);
                    b.swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.DoctorsResponse> call, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                b.swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading doctors", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadRatingStats() {
        ApiClient.get().getAllReviewStats().enqueue(new Callback<ResponseModels.AllReviewStatsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.AllReviewStatsResponse> call, Response<ResponseModels.AllReviewStatsResponse> response) {
                b.progressBar.setVisibility(View.GONE);
                b.swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().stats != null) {
                    // Merge rating stats with doctor list
                    for (UserModel doctor : fullDoctorList) {
                        ReviewStats stats = response.body().stats.get(doctor.id);
                        if (stats != null) {
                            doctor.averageRating = stats.average_rating;
                            doctor.totalReviews = stats.total_reviews;
                        }
                    }
                }
                filterList();
            }

            @Override
            public void onFailure(Call<ResponseModels.AllReviewStatsResponse> call, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                b.swipeRefresh.setRefreshing(false);
                // Still show doctors even if rating stats fail
                filterList();
            }
        });
    }

    private void filterList() {
        List<UserModel> filtered = new ArrayList<>();
        for (UserModel doctor : fullDoctorList) {
            boolean matchesSearch = doctor.name.toLowerCase().contains(searchQuery) ||
                (doctor.specializations != null && String.join(", ", doctor.specializations).toLowerCase().contains(searchQuery));
            
            boolean matchesCategory = categoryFilter.equals("All") || 
                (doctor.specializations != null && doctor.specializations.contains(categoryFilter));

            if (matchesSearch && matchesCategory) {
                filtered.add(doctor);
            }
        }

        b.layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        adapter = new DoctorAdapter(requireContext(), filtered, this);
        b.rvDoctors.setAdapter(adapter);
    }

    @Override
    public void onBook(UserModel doctor) {
        showBookingDialog(doctor);
    }

    @Override
    public void onDetails(UserModel doctor) {
        Intent intent = new Intent(getContext(), com.medreport.ai.activities.DoctorProfileActivity.class);
        intent.putExtra(com.medreport.ai.activities.DoctorProfileActivity.EXTRA_DOCTOR_ID, doctor.id);
        startActivity(intent);
    }

    private void showBookingDialog(UserModel doctor) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogBookAppointmentBinding db = DialogBookAppointmentBinding.inflate(getLayoutInflater());
        dialog.setContentView(db.getRoot());

        db.tvDoctorTarget.setText("With Dr. " + doctor.name);
        final String[] selectedDateTime = {""};

        db.btnPickDateTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                
                new TimePickerDialog(requireContext(), (view1, hourOfDay, minute) -> {
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);
                    
                    selectedDateTime[0] = String.format("%04d-%02d-%02dT%02d:%02d:00Z", 
                        year, month + 1, dayOfMonth, hourOfDay, minute);
                    db.tvSelectedDateTime.setText(String.format("Selected: %02d/%02d/%d at %02d:%02d", 
                        dayOfMonth, month + 1, year, hourOfDay, minute));
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
                
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });

        db.btnSubmitRequest.setOnClickListener(v -> {
            String notes = db.etNotes.getText().toString();
            if (selectedDateTime[0].isEmpty()) {
                Toast.makeText(getContext(), "Please select a date and time", Toast.LENGTH_SHORT).show();
                return;
            }

            db.progressSubmit.setVisibility(View.VISIBLE);
            db.btnSubmitRequest.setEnabled(false);

            Map<String, Object> body = new HashMap<>();
            body.put("doctor_id", doctor.id);
            body.put("notes", notes);
            body.put("preferred_time", selectedDateTime[0]);

            ApiClient.get().requestAppointment(body).enqueue(new Callback<ApiResponse<AppointmentModel>>() {
                @Override
                public void onResponse(Call<ApiResponse<AppointmentModel>> call, Response<ApiResponse<AppointmentModel>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Appointment request sent!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Failed to send request", Toast.LENGTH_SHORT).show();
                        db.progressSubmit.setVisibility(View.GONE);
                        db.btnSubmitRequest.setEnabled(true);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<AppointmentModel>> call, Throwable t) {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                    db.progressSubmit.setVisibility(View.GONE);
                    db.btnSubmitRequest.setEnabled(true);
                }
            });
        });

        dialog.show();
    }
}
