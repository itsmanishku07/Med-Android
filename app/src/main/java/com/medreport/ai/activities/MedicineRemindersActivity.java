package com.medreport.ai.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medreport.ai.R;
import com.medreport.ai.adapters.ReminderAdapter;
import com.medreport.ai.databinding.ActivityMedicineRemindersBinding;
import com.medreport.ai.models.ReminderModel;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.services.AlarmReceiver;
import com.medreport.ai.services.ReminderScheduler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.*;

public class MedicineRemindersActivity extends AppCompatActivity implements ReminderAdapter.Listener {
    private ActivityMedicineRemindersBinding b;
    private ReminderAdapter adapter;
    private final List<ReminderModel> reminders = new ArrayList<>();
    private static final String[] DAYS = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMedicineRemindersBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AlarmReceiver.stopAlarm(this);

        adapter = new ReminderAdapter(reminders, this);
        b.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerView.setAdapter(adapter);

        b.fab.setOnClickListener(v -> showReminderDialog(null));
        b.swipeRefresh.setOnRefreshListener(this::loadReminders);

        requestNotificationPermission();
        loadReminders();
    }

    private void loadReminders() {
        b.swipeRefresh.setRefreshing(true);
        ApiClient.get().getReminders().enqueue(new Callback<ResponseModels.RemindersResponse>() {
            @Override public void onResponse(Call<ResponseModels.RemindersResponse> c, Response<ResponseModels.RemindersResponse> r) {
                b.swipeRefresh.setRefreshing(false);
                if (r.isSuccessful() && r.body() != null && r.body().reminders != null) {
                    reminders.clear();
                    reminders.addAll(r.body().reminders);
                    adapter.notifyDataSetChanged();
                    b.tvEmpty.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
                    ReminderScheduler.scheduleAll(MedicineRemindersActivity.this, reminders);
                }
            }
            @Override public void onFailure(Call<ResponseModels.RemindersResponse> c, Throwable t) { b.swipeRefresh.setRefreshing(false); }
        });
    }

    private void showReminderDialog(ReminderModel existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reminder, null);
        EditText etName   = view.findViewById(R.id.etMedicineName);
        EditText etDosage = view.findViewById(R.id.etDosage);
        TimePicker timePicker = view.findViewById(R.id.timePicker);
        EditText etNotes  = view.findViewById(R.id.etNotes);
        CheckBox[] dayCbs = {
            view.findViewById(R.id.cbMon), view.findViewById(R.id.cbTue),
            view.findViewById(R.id.cbWed), view.findViewById(R.id.cbThu),
            view.findViewById(R.id.cbFri), view.findViewById(R.id.cbSat),
            view.findViewById(R.id.cbSun)
        };
        timePicker.setIs24HourView(false);

        if (existing != null) {
            etName.setText(existing.medicineName);
            etDosage.setText(existing.dosage);
            etNotes.setText(existing.notes);
            if (existing.reminderTime != null && existing.reminderTime.length() == 5) {
                String[] p = existing.reminderTime.split(":");
                timePicker.setHour(Integer.parseInt(p[0]));
                timePicker.setMinute(Integer.parseInt(p[1]));
            }
            if (existing.days != null) {
                for (int i = 0; i < DAYS.length; i++) {
                    dayCbs[i].setChecked(existing.days.contains(DAYS[i]));
                }
            }
        }

        new AlertDialog.Builder(this)
            .setTitle(existing == null ? "Add Reminder" : "Edit Reminder")
            .setView(view)
            .setPositiveButton("Save", (d, w) -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) { Toast.makeText(this, "Medicine name required", Toast.LENGTH_SHORT).show(); return; }
                String time = String.format(Locale.US, "%02d:%02d", timePicker.getHour(), timePicker.getMinute());
                List<String> days = new ArrayList<>();
                for (int i = 0; i < DAYS.length; i++) if (dayCbs[i].isChecked()) days.add(DAYS[i]);

                Map<String, Object> body = new HashMap<>();
                body.put("medicine_name", name);
                body.put("reminder_time", time);
                if (!etDosage.getText().toString().isEmpty()) body.put("dosage", etDosage.getText().toString().trim());
                if (!days.isEmpty()) body.put("days", days);
                if (!etNotes.getText().toString().isEmpty()) body.put("notes", etNotes.getText().toString().trim());

                if (existing == null) createReminder(body);
                else updateReminder(existing.id, body);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createReminder(Map<String, Object> body) {
        ApiClient.get().createReminder(body).enqueue(new Callback<ApiResponse<ReminderModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReminderModel>> c, Response<ApiResponse<ReminderModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().reminder != null) {
                    ReminderModel rm = r.body().reminder;
                    reminders.add(rm);
                    adapter.notifyItemInserted(reminders.size() - 1);
                    b.tvEmpty.setVisibility(View.GONE);
                    ReminderScheduler.schedule(MedicineRemindersActivity.this, rm);
                    Toast.makeText(MedicineRemindersActivity.this, "Reminder added", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<ReminderModel>> c, Throwable t) { Toast.makeText(MedicineRemindersActivity.this, "Failed", Toast.LENGTH_SHORT).show(); }
        });
    }

    private void updateReminder(String id, Map<String, Object> body) {
        ApiClient.get().updateReminder(id, body).enqueue(new Callback<ApiResponse<ReminderModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReminderModel>> c, Response<ApiResponse<ReminderModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().reminder != null) {
                    ReminderModel rm = r.body().reminder;
                    for (int i = 0; i < reminders.size(); i++) {
                        if (reminders.get(i).id.equals(id)) { reminders.set(i, rm); adapter.notifyItemChanged(i); break; }
                    }
                    if (rm.isActive) ReminderScheduler.schedule(MedicineRemindersActivity.this, rm);
                    else ReminderScheduler.cancel(MedicineRemindersActivity.this, id);
                    Toast.makeText(MedicineRemindersActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<ReminderModel>> c, Throwable t) {}
        });
    }

    @Override public void onEdit(ReminderModel r) { showReminderDialog(r); }

    @Override public void onDelete(ReminderModel r) {
        new AlertDialog.Builder(this).setTitle("Delete Reminder")
            .setMessage("Delete reminder for " + r.medicineName + "?")
            .setPositiveButton("Delete", (d, w) -> {
                ApiClient.get().deleteReminder(r.id).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> res) {
                        reminders.remove(r);
                        adapter.notifyDataSetChanged();
                        ReminderScheduler.cancel(MedicineRemindersActivity.this, r.id);
                        b.tvEmpty.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
                });
            }).setNegativeButton("Cancel", null).show();
    }

    @Override public void onToggle(ReminderModel r) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_active", !r.isActive);
        updateReminder(r.id, body);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                    .setTitle("Allow Exact Alarms")
                    .setMessage("To ring alarms at exact times, please allow exact alarms in settings.")
                    .setPositiveButton("Open Settings", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }).setNegativeButton("Later", null).show();
            }
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
