package com.medreport.ai.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.medreport.ai.adapters.AppointmentAdapter;
import com.medreport.ai.databinding.FragmentDoctorAppointmentsBinding;
import com.medreport.ai.models.AppointmentModel;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.network.ApiClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorAppointmentsFragment extends Fragment implements AppointmentAdapter.OnAppointmentInteractionListener {

    private FragmentDoctorAppointmentsBinding b;
    private List<AppointmentModel> fullList = new ArrayList<>();
    private AppointmentAdapter adapter;
    private String currentTab = "Pending";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        b = FragmentDoctorAppointmentsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupUI();
        loadAppointments();
    }

    private void setupUI() {
        b.rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(requireContext(), new ArrayList<>(), true, this);
        b.rvAppointments.setAdapter(adapter);

        b.swipeRefresh.setOnRefreshListener(this::loadAppointments);

        b.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText().toString();
                filterList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadAppointments() {
        b.progressBar.setVisibility(View.VISIBLE);
        ApiClient.get().getDoctorAppointments().enqueue(new Callback<ResponseModels.AppointmentsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.AppointmentsResponse> call, Response<ResponseModels.AppointmentsResponse> response) {
                b.progressBar.setVisibility(View.GONE);
                b.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    fullList = response.body().appointments;
                    filterList();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.AppointmentsResponse> call, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                b.swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading appointments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterList() {
        List<AppointmentModel> filtered = new ArrayList<>();
        for (AppointmentModel appt : fullList) {
            if (currentTab.equals("All")) filtered.add(appt);
            else if (currentTab.equals("Scheduled") && appt.isAccepted()) filtered.add(appt);
            else if (currentTab.equals("Pending") && appt.isPending()) filtered.add(appt);
        }

        adapter = new AppointmentAdapter(requireContext(), filtered, true, this);
        b.rvAppointments.setAdapter(adapter);
    }

    @Override
    public void onManage(AppointmentModel appointment) {
        showManageDialog(appointment);
    }

    @Override
    public void onItemClicked(AppointmentModel appointment) {
        // Future: Show appointment details
    }

    private void showManageDialog(AppointmentModel appt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(com.medreport.ai.R.layout.dialog_book_appointment, null);
        // Reuse booking dialog layout for scheduling by doctor
        
        // In a real app, I'd create a separate scheduling dialog layout. 
        // For now, I'll use a direct builder specifically for the doctor's schedule action.
        
        final String[] scheduleTime = {""};
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Confirm Appointment")
            .setMessage("Select date and time for consultation with " + appt.patientName)
            .setNeutralButton("Select Time", (dialog, which) -> {
                Calendar now = Calendar.getInstance();
                new DatePickerDialog(requireContext(), (view1, year, month, dayOfMonth) -> {
                    new TimePickerDialog(requireContext(), (view2, hourOfDay, minute) -> {
                        scheduleTime[0] = String.format("%04d-%02d-%02dT%02d:%02d:00Z", 
                            year, month + 1, dayOfMonth, hourOfDay, minute);
                        updateAppointment(appt.id, "ACCEPTED", scheduleTime[0], "");
                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
            })
            .setNegativeButton("Reject", (dialog, which) -> updateAppointment(appt.id, "REJECTED", null, ""))
            .setPositiveButton("Cancel", null)
            .show();
    }

    private void updateAppointment(String id, String status, String scheduledTime, String notes) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        if (scheduledTime != null) body.put("scheduled_at", scheduledTime);
        body.put("doctor_notes", notes);

        ApiClient.get().updateAppointmentStatus(id, body).enqueue(new Callback<ApiResponse<AppointmentModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<AppointmentModel>> call, Response<ApiResponse<AppointmentModel>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Appointment updated!", Toast.LENGTH_SHORT).show();
                    loadAppointments();
                } else {
                    Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AppointmentModel>> call, Throwable t) {
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
