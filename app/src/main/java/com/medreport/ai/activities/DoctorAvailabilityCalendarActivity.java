package com.medreport.ai.activities;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.medreport.ai.R;
import com.medreport.ai.databinding.ActivityDoctorAvailabilityCalendarBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import java.text.SimpleDateFormat;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorAvailabilityCalendarActivity extends AppCompatActivity {
    private ActivityDoctorAvailabilityCalendarBinding b;
    private Calendar currentMonth;
    private List<CalendarDay> calendarDays = new ArrayList<>();
    private CalendarAdapter calendarAdapter;
    private Map<String, List<AvailabilitySlot>> dateSlots = new HashMap<>();
    private List<AvailabilitySlot> weeklyTemplate = new ArrayList<>();
    private Set<String> blockedDates = new HashSet<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityDoctorAvailabilityCalendarBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        currentMonth = Calendar.getInstance();
        setupToolbar();
        setupCalendar();
        setupListeners();
        loadCalendarData();
    }

    private void setupToolbar() {
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Availability Calendar");
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCalendar() {
        calendarAdapter = new CalendarAdapter();
        b.rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        b.rvCalendar.setAdapter(calendarAdapter);
        updateCalendarHeader();
        generateCalendarDays();
    }

    private void setupListeners() {
        b.btnPreviousMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateCalendarHeader();
            generateCalendarDays();
            loadCalendarData();
        });

        b.btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateCalendarHeader();
            generateCalendarDays();
            loadCalendarData();
        });

        b.btnToday.setOnClickListener(v -> {
            currentMonth = Calendar.getInstance();
            updateCalendarHeader();
            generateCalendarDays();
            loadCalendarData();
        });

        b.btnApplyTemplate.setOnClickListener(v -> showApplyTemplateDialog());
        
        b.btnWeeklyView.setOnClickListener(v -> finish());
    }

    private void updateCalendarHeader() {
        b.tvMonthYear.setText(monthYearFormat.format(currentMonth.getTime()));
    }

    private void generateCalendarDays() {
        calendarDays.clear();
        
        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Add previous month days
        cal.add(Calendar.DAY_OF_MONTH, -(firstDayOfWeek - 1));
        for (int i = 0; i < firstDayOfWeek - 1; i++) {
            calendarDays.add(new CalendarDay(cal.getTime(), false));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Add current month days
        cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        for (int i = 0; i < daysInMonth; i++) {
            calendarDays.add(new CalendarDay(cal.getTime(), true));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Add next month days to fill grid
        while (calendarDays.size() % 7 != 0) {
            calendarDays.add(new CalendarDay(cal.getTime(), false));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        calendarAdapter.notifyDataSetChanged();
    }

    private void loadCalendarData() {
        Calendar start = (Calendar) currentMonth.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        
        Calendar end = (Calendar) currentMonth.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        
        String startDate = dateFormat.format(start.getTime());
        String endDate = dateFormat.format(end.getTime());
        
        ApiClient.get().getCalendarSlots(startDate, endDate).enqueue(new Callback<ResponseModels.CalendarSlotsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.CalendarSlotsResponse> call, Response<ResponseModels.CalendarSlotsResponse> r) {
                if (r.isSuccessful() && r.body() != null) {
                    dateSlots.clear();
                    blockedDates.clear();
                    
                    if (r.body().slots != null) {
                        for (AvailabilitySlot slot : r.body().slots) {
                            if (slot.date != null) {
                                String dateKey = slot.date.split("T")[0];
                                if (!dateSlots.containsKey(dateKey)) {
                                    dateSlots.put(dateKey, new ArrayList<>());
                                }
                                dateSlots.get(dateKey).add(slot);
                            }
                        }
                    }
                    
                    if (r.body().weeklyTemplate != null) {
                        weeklyTemplate.clear();
                        weeklyTemplate.addAll(r.body().weeklyTemplate);
                    }
                    
                    if (r.body().blockedDates != null) {
                        for (BlockedDate blocked : r.body().blockedDates) {
                            if (blocked.date != null) {
                                blockedDates.add(blocked.date.split("T")[0]);
                            }
                        }
                    }
                    
                    calendarAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.CalendarSlotsResponse> call, Throwable t) {
                Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Failed to load calendar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDateDetailsDialog(CalendarDay day) {
        if (!day.isCurrentMonth) return;
        
        String dateKey = dateFormat.format(day.date);
        List<AvailabilitySlot> slots = getDisplaySlotsForDate(day.date);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_date_availability, null);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        RecyclerView rvSlots = dialogView.findViewById(R.id.rvSlots);
        Button btnAddSlot = dialogView.findViewById(R.id.btnAddSlot);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);
        tvDate.setText(displayFormat.format(day.date));
        
        DateSlotsAdapter slotsAdapter = new DateSlotsAdapter(slots, slot -> {
            if (!slot.isTemplate) {
                confirmDeleteSlot(slot);
            }
        });
        rvSlots.setLayoutManager(new LinearLayoutManager(this));
        rvSlots.setAdapter(slotsAdapter);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        
        btnAddSlot.setOnClickListener(v -> {
            dialog.dismiss();
            showAddSlotForDateDialog(dateKey);
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private List<AvailabilitySlot> getDisplaySlotsForDate(Date date) {
        String dateKey = dateFormat.format(date);
        List<AvailabilitySlot> slots = new ArrayList<>();
        
        // Check date-specific slots
        if (dateSlots.containsKey(dateKey)) {
            for (AvailabilitySlot slot : dateSlots.get(dateKey)) {
                slot.isTemplate = false;
                slots.add(slot);
            }
        }
        
        // If no date-specific slots, check weekly template
        if (slots.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            String dayName = new SimpleDateFormat("EEEE", Locale.US).format(date);
            
            for (AvailabilitySlot slot : weeklyTemplate) {
                if (slot.dayOfWeek != null && slot.dayOfWeek.equals(dayName)) {
                    AvailabilitySlot templateSlot = new AvailabilitySlot();
                    templateSlot.id = slot.id;
                    templateSlot.startTime = slot.startTime;
                    templateSlot.endTime = slot.endTime;
                    templateSlot.maxAppointments = slot.maxAppointments;
                    templateSlot.isTemplate = true;
                    slots.add(templateSlot);
                }
            }
        }
        
        return slots;
    }

    private void showAddSlotForDateDialog(String date) {
        final String[] startTime = {"09:00"};
        final String[] endTime = {"17:00"};
        final int[] maxAppointments = {10};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView startLabel = new TextView(this);
        startLabel.setText("Start Time");
        startLabel.setTextSize(14);
        layout.addView(startLabel);
        
        Button btnStartTime = new Button(this);
        btnStartTime.setText(formatTime(startTime[0]));
        btnStartTime.setOnClickListener(v -> showTimePicker(time -> {
            startTime[0] = time;
            btnStartTime.setText(formatTime(time));
        }));
        layout.addView(btnStartTime);

        TextView endLabel = new TextView(this);
        endLabel.setText("End Time");
        endLabel.setTextSize(14);
        endLabel.setPadding(0, 20, 0, 0);
        layout.addView(endLabel);
        
        Button btnEndTime = new Button(this);
        btnEndTime.setText(formatTime(endTime[0]));
        btnEndTime.setOnClickListener(v -> showTimePicker(time -> {
            endTime[0] = time;
            btnEndTime.setText(formatTime(time));
        }));
        layout.addView(btnEndTime);

        TextView maxLabel = new TextView(this);
        maxLabel.setText("Max Appointments");
        maxLabel.setTextSize(14);
        maxLabel.setPadding(0, 20, 0, 0);
        layout.addView(maxLabel);
        
        EditText etMaxAppointments = new EditText(this);
        etMaxAppointments.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMaxAppointments.setText("10");
        layout.addView(etMaxAppointments);

        new MaterialAlertDialogBuilder(this)
                .setView(layout)
                .setTitle("Add Availability")
                .setPositiveButton("Add", (dialog, which) -> {
                    try {
                        maxAppointments[0] = Integer.parseInt(etMaxAppointments.getText().toString());
                    } catch (Exception e) {
                        maxAppointments[0] = 10;
                    }
                    createCalendarSlot(date, startTime[0], endTime[0], maxAppointments[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showApplyTemplateDialog() {
        if (weeklyTemplate.isEmpty()) {
            Toast.makeText(this, "Please set up weekly template first", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView label = new TextView(this);
        label.setText("Number of weeks to apply template:");
        label.setTextSize(14);
        layout.addView(label);
        
        EditText etWeeks = new EditText(this);
        etWeeks.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etWeeks.setText("4");
        layout.addView(etWeeks);

        TextView infoLabel = new TextView(this);
        infoLabel.setText("\nThis will create availability slots for the next N weeks based on your weekly template.");
        infoLabel.setTextSize(12);
        infoLabel.setTextColor(Color.GRAY);
        layout.addView(infoLabel);

        new MaterialAlertDialogBuilder(this)
                .setView(layout)
                .setTitle("Apply Weekly Template")
                .setPositiveButton("Apply", (dialog, which) -> {
                    try {
                        int weeks = Integer.parseInt(etWeeks.getText().toString());
                        if (weeks > 0 && weeks <= 52) {
                            applyWeeklyTemplate(weeks);
                        } else {
                            Toast.makeText(this, "Please enter 1-52 weeks", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createCalendarSlot(String date, String startTime, String endTime, int maxAppointments) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        body.put("start_time", startTime);
        body.put("end_time", endTime);
        body.put("max_appointments", maxAppointments);

        ApiClient.get().createCalendarSlot(body).enqueue(new Callback<ApiResponse<AvailabilitySlot>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilitySlot>> call, Response<ApiResponse<AvailabilitySlot>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Availability added", Toast.LENGTH_SHORT).show();
                    loadCalendarData();
                } else {
                    Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Failed to add availability", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilitySlot>> call, Throwable t) {
                Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Failed to add availability", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyWeeklyTemplate(int weeks) {
        Calendar startCal = Calendar.getInstance();
        startCal.add(Calendar.DAY_OF_MONTH, 1); // Start from tomorrow
        String startDate = dateFormat.format(startCal.getTime());

        Map<String, Object> body = new HashMap<>();
        body.put("start_date", startDate);
        body.put("weeks", weeks);

        ApiClient.get().applyWeeklyTemplate(body).enqueue(new Callback<ResponseModels.ApplyTemplateResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.ApplyTemplateResponse> call, Response<ResponseModels.ApplyTemplateResponse> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Toast.makeText(DoctorAvailabilityCalendarActivity.this, 
                            "Template applied! Created " + r.body().slotsCreated + " slots", 
                            Toast.LENGTH_LONG).show();
                    loadCalendarData();
                } else {
                    Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Failed to apply template", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.ApplyTemplateResponse> call, Throwable t) {
                Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Failed to apply template", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteSlot(AvailabilitySlot slot) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Slot")
                .setMessage("Remove " + slot.getTimeRange() + "?")
                .setPositiveButton("Delete", (d, w) -> deleteCalendarSlot(slot.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCalendarSlot(String slotId) {
        ApiClient.get().deleteCalendarSlot(slotId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Slot deleted", Toast.LENGTH_SHORT).show();
                    loadCalendarData();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DoctorAvailabilityCalendarActivity.this, "Failed to delete slot", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTimePicker(TimePickerCallback callback) {
        Calendar cal = Calendar.getInstance();
        TimePickerDialog picker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> callback.onTimePicked(String.format(Locale.US, "%02d:%02d", hourOfDay, minute)),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
        picker.show();
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

    private boolean hasAvailability(Date date) {
        String dateKey = dateFormat.format(date);
        
        // Check date-specific slots
        if (dateSlots.containsKey(dateKey) && !dateSlots.get(dateKey).isEmpty()) {
            return true;
        }
        
        // Check weekly template
        String dayName = new SimpleDateFormat("EEEE", Locale.US).format(date);
        for (AvailabilitySlot slot : weeklyTemplate) {
            if (slot.dayOfWeek != null && slot.dayOfWeek.equals(dayName)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean isDateBlocked(Date date) {
        String dateKey = dateFormat.format(date);
        return blockedDates.contains(dateKey);
    }

    private boolean isDatePast(Date date) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return date.before(today.getTime());
    }

    private boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR);
    }

    // Calendar Day Model
    static class CalendarDay {
        Date date;
        boolean isCurrentMonth;

        CalendarDay(Date date, boolean isCurrentMonth) {
            this.date = date;
            this.isCurrentMonth = isCurrentMonth;
        }
    }

    // Calendar Adapter
    class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CalendarDay day = calendarDays.get(position);
            Calendar cal = Calendar.getInstance();
            cal.setTime(day.date);
            
            holder.tvDay.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            
            boolean isPast = isDatePast(day.date);
            boolean isBlocked = isDateBlocked(day.date);
            boolean hasSlots = hasAvailability(day.date);
            boolean today = isToday(day.date);
            
            // Set text color
            if (!day.isCurrentMonth) {
                holder.tvDay.setTextColor(Color.LTGRAY);
            } else if (isPast) {
                holder.tvDay.setTextColor(Color.GRAY);
            } else if (today) {
                holder.tvDay.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else {
                holder.tvDay.setTextColor(Color.BLACK);
            }
            
            // Set background color
            if (!day.isCurrentMonth) {
                holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            } else if (isBlocked) {
                holder.card.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            } else if (hasSlots) {
                holder.card.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            } else {
                holder.card.setCardBackgroundColor(Color.WHITE);
            }
            
            // Show indicator dot
            holder.indicator.setVisibility(day.isCurrentMonth && hasSlots && !isBlocked ? View.VISIBLE : View.GONE);
            
            // Set border for today
            if (today) {
                holder.card.setStrokeColor(getResources().getColor(android.R.color.holo_blue_dark));
                holder.card.setStrokeWidth(4);
            } else {
                holder.card.setStrokeWidth(0);
            }
            
            // Click listener
            holder.itemView.setOnClickListener(v -> {
                if (!isPast && day.isCurrentMonth) {
                    showDateDetailsDialog(day);
                }
            });
            
            holder.itemView.setEnabled(!isPast && day.isCurrentMonth);
        }

        @Override
        public int getItemCount() {
            return calendarDays.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView tvDay;
            View indicator;

            ViewHolder(View view) {
                super(view);
                card = view.findViewById(R.id.cardDay);
                tvDay = view.findViewById(R.id.tvDay);
                indicator = view.findViewById(R.id.indicator);
            }
        }
    }

    // Date Slots Adapter
    class DateSlotsAdapter extends RecyclerView.Adapter<DateSlotsAdapter.ViewHolder> {
        List<AvailabilitySlot> slots;
        OnSlotClickListener listener;

        DateSlotsAdapter(List<AvailabilitySlot> slots, OnSlotClickListener listener) {
            this.slots = slots;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AvailabilitySlot slot = slots.get(position);
            holder.tvTime.setText(slot.getTimeRange());
            holder.tvMaxAppointments.setText("Max " + slot.maxAppointments + " appointments");
            
            if (slot.isTemplate) {
                holder.tvTemplateLabel.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.GONE);
            } else {
                holder.tvTemplateLabel.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> listener.onSlotClick(slot));
            }
        }

        @Override
        public int getItemCount() {
            return slots.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvMaxAppointments, tvTemplateLabel;
            ImageButton btnDelete;

            ViewHolder(View view) {
                super(view);
                tvTime = view.findViewById(R.id.tvTime);
                tvMaxAppointments = view.findViewById(R.id.tvMaxAppointments);
                tvTemplateLabel = view.findViewById(R.id.tvTemplateLabel);
                btnDelete = view.findViewById(R.id.btnDelete);
            }
        }

        interface OnSlotClickListener {
            void onSlotClick(AvailabilitySlot slot);
        }
    }

    interface TimePickerCallback {
        void onTimePicked(String time);
    }
}
