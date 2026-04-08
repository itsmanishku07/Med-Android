package com.medreport.ai.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medreport.ai.R;
import com.medreport.ai.adapters.AvailabilitySlotAdapter;
import com.medreport.ai.adapters.BlockedDateAdapter;
import com.medreport.ai.databinding.ActivityDoctorAvailabilityBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorAvailabilityActivity extends AppCompatActivity {
    private ActivityDoctorAvailabilityBinding b;
    private AvailabilitySlotAdapter slotAdapter;
    private BlockedDateAdapter blockedDateAdapter;
    private List<AvailabilitySlot> slots = new ArrayList<>();
    private List<BlockedDate> blockedDates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityDoctorAvailabilityBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setupToolbar();
        setupRecyclerViews();
        setupListeners();
        loadData();
    }

    private void setupToolbar() {
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Availability");
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        slotAdapter = new AvailabilitySlotAdapter(slots, new AvailabilitySlotAdapter.Listener() {
            @Override
            public void onEdit(AvailabilitySlot slot) {
                showEditSlotDialog(slot);
            }

            @Override
            public void onDelete(AvailabilitySlot slot) {
                confirmDeleteSlot(slot);
            }

            @Override
            public void onToggle(AvailabilitySlot slot) {
                toggleSlotAvailability(slot);
            }
        });
        b.rvSlots.setLayoutManager(new LinearLayoutManager(this));
        b.rvSlots.setAdapter(slotAdapter);

        blockedDateAdapter = new BlockedDateAdapter(blockedDates, blockedDate -> confirmDeleteBlockedDate(blockedDate));
        b.rvBlockedDates.setLayoutManager(new LinearLayoutManager(this));
        b.rvBlockedDates.setAdapter(blockedDateAdapter);
    }

    private void setupListeners() {
        b.fabAddSlot.setOnClickListener(v -> showAddSlotDialog());
        b.fabAddBlockedDate.setOnClickListener(v -> showAddBlockedDateDialog());
        b.swipeRefresh.setOnRefreshListener(this::loadData);
        
        // Calendar view button
        b.btnCalendarView.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, DoctorAvailabilityCalendarActivity.class));
        });
    }

    private void loadData() {
        b.swipeRefresh.setRefreshing(true);
        loadSlots();
        loadBlockedDates();
    }

    private void loadSlots() {
        ApiClient.get().getMyAvailabilitySlots().enqueue(new Callback<ResponseModels.AvailabilitySlotsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.AvailabilitySlotsResponse> call, Response<ResponseModels.AvailabilitySlotsResponse> r) {
                b.swipeRefresh.setRefreshing(false);
                if (r.isSuccessful() && r.body() != null && r.body().slots != null) {
                    slots.clear();
                    slots.addAll(r.body().slots);
                    slotAdapter.notifyDataSetChanged();
                    b.tvEmptySlots.setVisibility(slots.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.AvailabilitySlotsResponse> call, Throwable t) {
                b.swipeRefresh.setRefreshing(false);
                Toast.makeText(DoctorAvailabilityActivity.this, "Failed to load slots", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBlockedDates() {
        ApiClient.get().getMyBlockedDates().enqueue(new Callback<ResponseModels.BlockedDatesResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.BlockedDatesResponse> call, Response<ResponseModels.BlockedDatesResponse> r) {
                if (r.isSuccessful() && r.body() != null && r.body().blockedDates != null) {
                    blockedDates.clear();
                    blockedDates.addAll(r.body().blockedDates);
                    blockedDateAdapter.notifyDataSetChanged();
                    b.tvEmptyBlocked.setVisibility(blockedDates.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.BlockedDatesResponse> call, Throwable t) {
            }
        });
    }

    private void showAddSlotDialog() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        final String[] selectedDay = {days[0]};
        final String[] startTime = {"09:00"};
        final String[] endTime = {"17:00"};
        final int[] maxAppointments = {10};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(android.R.layout.select_dialog_item, null);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        android.widget.TextView dayLabel = new android.widget.TextView(this);
        dayLabel.setText("Day of Week");
        dayLabel.setTextSize(14);
        layout.addView(dayLabel);
        
        android.widget.Spinner spinnerDay = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapter);
        spinnerDay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedDay[0] = days[position];
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        layout.addView(spinnerDay);

        android.widget.TextView startLabel = new android.widget.TextView(this);
        startLabel.setText("Start Time");
        startLabel.setTextSize(14);
        startLabel.setPadding(0, 20, 0, 0);
        layout.addView(startLabel);
        
        android.widget.Button btnStartTime = new android.widget.Button(this);
        btnStartTime.setText(formatTime(startTime[0]));
        btnStartTime.setOnClickListener(v -> showTimePicker(time -> {
            startTime[0] = time;
            btnStartTime.setText(formatTime(time));
        }));
        layout.addView(btnStartTime);

        android.widget.TextView endLabel = new android.widget.TextView(this);
        endLabel.setText("End Time");
        endLabel.setTextSize(14);
        endLabel.setPadding(0, 20, 0, 0);
        layout.addView(endLabel);
        
        android.widget.Button btnEndTime = new android.widget.Button(this);
        btnEndTime.setText(formatTime(endTime[0]));
        btnEndTime.setOnClickListener(v -> showTimePicker(time -> {
            endTime[0] = time;
            btnEndTime.setText(formatTime(time));
        }));
        layout.addView(btnEndTime);

        android.widget.TextView maxLabel = new android.widget.TextView(this);
        maxLabel.setText("Max Appointments");
        maxLabel.setTextSize(14);
        maxLabel.setPadding(0, 20, 0, 0);
        layout.addView(maxLabel);
        
        android.widget.EditText etMaxAppointments = new android.widget.EditText(this);
        etMaxAppointments.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMaxAppointments.setText("10");
        layout.addView(etMaxAppointments);

        builder.setView(layout);
        builder.setTitle("Add Availability Slot")
                .setPositiveButton("Add", (dialog, which) -> {
                    try {
                        maxAppointments[0] = Integer.parseInt(etMaxAppointments.getText().toString());
                    } catch (Exception e) {
                        maxAppointments[0] = 10;
                    }
                    createSlot(selectedDay[0], startTime[0], endTime[0], maxAppointments[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditSlotDialog(AvailabilitySlot slot) {
        final String[] startTime = {slot.startTime};
        final String[] endTime = {slot.endTime};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_availability_slot, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputEditText etDayOfWeek = dialogView.findViewById(R.id.etDayOfWeek);
        com.google.android.material.textfield.TextInputEditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        com.google.android.material.textfield.TextInputEditText etEndTime = dialogView.findViewById(R.id.etEndTime);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btnSave);

        etDayOfWeek.setText(slot.getDayDisplay());
        etStartTime.setText(formatTime(startTime[0]));
        etEndTime.setText(formatTime(endTime[0]));

        etStartTime.setOnClickListener(v -> showTimePicker(time -> {
            startTime[0] = time;
            etStartTime.setText(formatTime(time));
        }));

        etEndTime.setOnClickListener(v -> showTimePicker(time -> {
            endTime[0] = time;
            etEndTime.setText(formatTime(time));
        }));

        AlertDialog dialog = builder.create();
        
        btnSave.setOnClickListener(v -> {
            updateSlot(slot.id, startTime[0], endTime[0]);
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showAddBlockedDateDialog() {
        android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(this);
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        datePicker.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            showBlockedDateReasonDialog(date);
        });
        datePicker.show();
    }

    private void showBlockedDateReasonDialog(String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Reason (optional)");
        input.setPadding(50, 40, 50, 10);
        builder.setView(input);
        builder.setTitle("Block Date: " + date)
                .setPositiveButton("Block", (dialog, which) -> createBlockedDate(date, input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTimePicker(TimePickerCallback callback) {
        Calendar cal = Calendar.getInstance();
        TimePickerDialog picker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> callback.onTimePicked(String.format(Locale.US, "%02d:%02d", hourOfDay, minute)),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
        picker.show();
    }

    private void createSlot(String day, String startTime, String endTime, int maxAppointments) {
        Map<String, Object> body = new HashMap<>();
        body.put("day_of_week", day);
        body.put("start_time", startTime);
        body.put("end_time", endTime);
        body.put("max_appointments", maxAppointments);

        ApiClient.get().createAvailabilitySlot(body).enqueue(new Callback<ApiResponse<AvailabilitySlot>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilitySlot>> call, Response<ApiResponse<AvailabilitySlot>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DoctorAvailabilityActivity.this, "Slot added", Toast.LENGTH_SHORT).show();
                    loadSlots();
                } else {
                    Toast.makeText(DoctorAvailabilityActivity.this, "Failed to add slot", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilitySlot>> call, Throwable t) {
                Toast.makeText(DoctorAvailabilityActivity.this, "Failed to add slot", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSlot(String slotId, String startTime, String endTime) {
        Map<String, Object> body = new HashMap<>();
        body.put("start_time", startTime);
        body.put("end_time", endTime);

        ApiClient.get().updateAvailabilitySlot(slotId, body).enqueue(new Callback<ApiResponse<AvailabilitySlot>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilitySlot>> call, Response<ApiResponse<AvailabilitySlot>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DoctorAvailabilityActivity.this, "Slot updated", Toast.LENGTH_SHORT).show();
                    loadSlots();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilitySlot>> call, Throwable t) {
                Toast.makeText(DoctorAvailabilityActivity.this, "Failed to update slot", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleSlotAvailability(AvailabilitySlot slot) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_available", !slot.isAvailable);

        ApiClient.get().updateAvailabilitySlot(slot.id, body).enqueue(new Callback<ApiResponse<AvailabilitySlot>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilitySlot>> call, Response<ApiResponse<AvailabilitySlot>> r) {
                if (r.isSuccessful()) {
                    loadSlots();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilitySlot>> call, Throwable t) {
            }
        });
    }

    private void confirmDeleteSlot(AvailabilitySlot slot) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Slot")
                .setMessage("Remove " + slot.getDayDisplay() + " " + slot.getTimeRange() + "?")
                .setPositiveButton("Delete", (d, w) -> deleteSlot(slot.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSlot(String slotId) {
        ApiClient.get().deleteAvailabilitySlot(slotId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DoctorAvailabilityActivity.this, "Slot deleted", Toast.LENGTH_SHORT).show();
                    loadSlots();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
            }
        });
    }

    private void createBlockedDate(String date, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        body.put("reason", reason);
        body.put("all_day", true);

        ApiClient.get().createBlockedDate(body).enqueue(new Callback<ApiResponse<BlockedDate>>() {
            @Override
            public void onResponse(Call<ApiResponse<BlockedDate>> call, Response<ApiResponse<BlockedDate>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DoctorAvailabilityActivity.this, "Date blocked", Toast.LENGTH_SHORT).show();
                    loadBlockedDates();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<BlockedDate>> call, Throwable t) {
                Toast.makeText(DoctorAvailabilityActivity.this, "Failed to block date", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteBlockedDate(BlockedDate blockedDate) {
        new AlertDialog.Builder(this)
                .setTitle("Unblock Date")
                .setMessage("Remove block for " + blockedDate.getDisplayDate() + "?")
                .setPositiveButton("Unblock", (d, w) -> deleteBlockedDate(blockedDate.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBlockedDate(String blockedDateId) {
        ApiClient.get().deleteBlockedDate(blockedDateId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DoctorAvailabilityActivity.this, "Date unblocked", Toast.LENGTH_SHORT).show();
                    loadBlockedDates();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
            }
        });
    }

    private String formatTime(String time24) {
        try {
            String[] parts = time24.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            String ampm = h >= 12 ? "PM" : "AM";
            int h12 = h % 12 == 0 ? 12 : h % 12;
            return String.format("%d:%02d %s", h12, m, ampm);
        } catch (Exception e) {
            return time24;
        }
    }

    interface TimePickerCallback {
        void onTimePicked(String time);
    }
}
