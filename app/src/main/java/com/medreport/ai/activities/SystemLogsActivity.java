package com.medreport.ai.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.medreport.ai.R;
import com.medreport.ai.adapters.SystemLogAdapter;
import com.medreport.ai.databinding.ActivitySystemLogsBinding;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class SystemLogsActivity extends AppCompatActivity {

    private ActivitySystemLogsBinding b;
    private SystemLogAdapter logAdapter;
    private int currentPage = 1;
    private int selectedHours = 24;
    private String selectedLevel = "";
    private ResponseModels.LogStatistics statistics;
    private ResponseModels.LogSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySystemLogsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupFilters();
        setupListeners();
        setupCharts();
        
        loadSettings();
        loadStatistics();
        loadLogs();
    }

    private void setupToolbar() {
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("System Logs");
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        logAdapter = new SystemLogAdapter();
        b.rvLogs.setLayoutManager(new LinearLayoutManager(this));
        b.rvLogs.setAdapter(logAdapter);
    }

    private void setupFilters() {
        // Hours filter
        String[] hoursOptions = {"Last Hour", "Last 6 Hours", "Last 24 Hours", "Last Week"};
        ArrayAdapter<String> hoursAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, hoursOptions);
        hoursAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerHours.setAdapter(hoursAdapter);
        b.spinnerHours.setSelection(2); // Default: Last 24 Hours
        
        b.spinnerHours.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: selectedHours = 1; break;
                    case 1: selectedHours = 6; break;
                    case 2: selectedHours = 24; break;
                    case 3: selectedHours = 168; break;
                }
                currentPage = 1;
                loadStatistics();
                loadLogs();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Level filter
        String[] levelOptions = {"All Levels", "Errors", "Warnings", "Info"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, levelOptions);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerLevel.setAdapter(levelAdapter);
        
        b.spinnerLevel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: selectedLevel = ""; break;
                    case 1: selectedLevel = "ERROR"; break;
                    case 2: selectedLevel = "WARNING"; break;
                    case 3: selectedLevel = "INFO"; break;
                }
                currentPage = 1;
                loadLogs();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupListeners() {
        b.btnRefresh.setOnClickListener(v -> {
            loadSettings();
            loadStatistics();
            loadLogs();
        });

        b.btnSettings.setOnClickListener(v -> showSettingsDialog());
        b.btnClear.setOnClickListener(v -> showClearConfirmDialog());
        b.btnDownload.setOnClickListener(v -> downloadLogs());
    }

    private void setupCharts() {
        // Line Chart setup
        b.chartRequestsOverTime.getDescription().setEnabled(false);
        b.chartRequestsOverTime.setTouchEnabled(true);
        b.chartRequestsOverTime.setDragEnabled(true);
        b.chartRequestsOverTime.setScaleEnabled(true);
        b.chartRequestsOverTime.setPinchZoom(true);
        b.chartRequestsOverTime.setDrawGridBackground(false);
        b.chartRequestsOverTime.getLegend().setEnabled(false);
        
        // Pie Chart setup
        b.chartStatusCodes.getDescription().setEnabled(false);
        b.chartStatusCodes.setUsePercentValues(true);
        b.chartStatusCodes.setDrawHoleEnabled(true);
        b.chartStatusCodes.setHoleColor(Color.WHITE);
        b.chartStatusCodes.setTransparentCircleRadius(61f);
        b.chartStatusCodes.setDrawEntryLabels(false);
        
        // Bar Chart setup
        b.chartTopEndpoints.getDescription().setEnabled(false);
        b.chartTopEndpoints.setDrawGridBackground(false);
        b.chartTopEndpoints.setDrawBarShadow(false);
        b.chartTopEndpoints.setDrawValueAboveBar(true);
        b.chartTopEndpoints.getLegend().setEnabled(false);
        b.chartTopEndpoints.getAxisLeft().setEnabled(false);
        b.chartTopEndpoints.getAxisRight().setDrawGridLines(false);
    }

    private void loadSettings() {
        ApiClient.get().getLogSettings().enqueue(new Callback<ResponseModels.LogSettingsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.LogSettingsResponse> call, Response<ResponseModels.LogSettingsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    settings = response.body().settings;
                    updateLoggingStatus();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.LogSettingsResponse> call, Throwable t) {
                Toast.makeText(SystemLogsActivity.this, "Failed to load settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatistics() {
        b.progressBar.setVisibility(View.VISIBLE);
        
        ApiClient.get().getLogStatistics(selectedHours).enqueue(new Callback<ResponseModels.LogStatisticsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.LogStatisticsResponse> call, Response<ResponseModels.LogStatisticsResponse> response) {
                b.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    statistics = response.body().statistics;
                    updateStatisticsUI();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.LogStatisticsResponse> call, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                Toast.makeText(SystemLogsActivity.this, "Failed to load statistics", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLogs() {
        b.progressBar.setVisibility(View.VISIBLE);
        
        ApiClient.get().getSystemLogs(currentPage, 50).enqueue(new Callback<ResponseModels.SystemLogsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.SystemLogsResponse> call, Response<ResponseModels.SystemLogsResponse> response) {
                b.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    logAdapter.setLogs(response.body().logs);
                    
                    if (response.body().logs == null || response.body().logs.isEmpty()) {
                        b.tvNoLogs.setVisibility(View.VISIBLE);
                        b.rvLogs.setVisibility(View.GONE);
                    } else {
                        b.tvNoLogs.setVisibility(View.GONE);
                        b.rvLogs.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(SystemLogsActivity.this, "Failed to load logs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.SystemLogsResponse> call, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                Toast.makeText(SystemLogsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLoggingStatus() {
        if (settings != null) {
            if (settings.enabled) {
                b.tvLoggingStatus.setText("Logging Active");
                b.tvLoggingStatus.setBackgroundResource(R.drawable.bg_success_badge);
                b.layoutWarning.setVisibility(View.GONE);
            } else {
                b.tvLoggingStatus.setText("Logging Disabled");
                b.tvLoggingStatus.setBackgroundResource(R.drawable.bg_gray_badge);
                b.layoutWarning.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateStatisticsUI() {
        if (statistics != null) {
            b.tvTotalRequests.setText(String.valueOf(statistics.totalRequests));
            b.tvTotalErrors.setText(String.valueOf(statistics.totalErrors));
            b.tvTotalWarnings.setText(String.valueOf(statistics.totalWarnings));
            b.tvAvgResponse.setText(String.format("%.0fms", statistics.avgResponseTime));
            
            // Update charts
            updateRequestsChart();
            updateStatusCodesChart();
            updateEndpointsChart();
            updateRecentErrors();
        }
    }

    private void updateRequestsChart() {
        if (statistics.requestsByHour == null || statistics.requestsByHour.isEmpty()) {
            b.chartRequestsOverTime.setVisibility(View.GONE);
            return;
        }
        
        b.chartRequestsOverTime.setVisibility(View.VISIBLE);
        
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        // Sort by timestamp
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(statistics.requestsByHour.entrySet());
        Collections.sort(sortedEntries, (a, b) -> a.getKey().compareTo(b.getKey()));
        
        int index = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            entries.add(new Entry(index, entry.getValue()));
            try {
                Date date = new Date(entry.getKey());
                labels.add(sdf.format(date));
            } catch (Exception e) {
                labels.add(entry.getKey());
            }
            index++;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Requests");
        dataSet.setColor(Color.parseColor("#3B82F6"));
        dataSet.setCircleColor(Color.parseColor("#3B82F6"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#3B82F6"));
        dataSet.setFillAlpha(50);
        
        LineData lineData = new LineData(dataSet);
        b.chartRequestsOverTime.setData(lineData);
        
        XAxis xAxis = b.chartRequestsOverTime.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        
        b.chartRequestsOverTime.invalidate();
    }

    private void updateStatusCodesChart() {
        if (statistics.statusCodes == null || statistics.statusCodes.isEmpty()) {
            b.chartStatusCodes.setVisibility(View.GONE);
            return;
        }
        
        b.chartStatusCodes.setVisibility(View.VISIBLE);
        
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : statistics.statusCodes.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Status Codes");
        
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#3B82F6")); // Blue
        colors.add(Color.parseColor("#10B981")); // Green
        colors.add(Color.parseColor("#F59E0B")); // Orange
        colors.add(Color.parseColor("#EF4444")); // Red
        colors.add(Color.parseColor("#8B5CF6")); // Purple
        dataSet.setColors(colors);
        
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });
        
        b.chartStatusCodes.setData(pieData);
        b.chartStatusCodes.invalidate();
    }

    private void updateEndpointsChart() {
        if (statistics.requestsByEndpoint == null || statistics.requestsByEndpoint.isEmpty()) {
            b.chartTopEndpoints.setVisibility(View.GONE);
            return;
        }
        
        b.chartTopEndpoints.setVisibility(View.VISIBLE);
        
        // Sort and get top 10
        List<Map.Entry<String, Integer>> sortedEndpoints = new ArrayList<>(statistics.requestsByEndpoint.entrySet());
        Collections.sort(sortedEndpoints, (a, b) -> b.getValue().compareTo(a.getValue()));
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int count = Math.min(10, sortedEndpoints.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Integer> entry = sortedEndpoints.get(i);
            entries.add(new BarEntry(i, entry.getValue()));
            
            String endpoint = entry.getKey();
            if (endpoint.length() > 30) {
                endpoint = endpoint.substring(0, 27) + "...";
            }
            labels.add(endpoint);
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Requests");
        dataSet.setColor(Color.parseColor("#3B82F6"));
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        
        b.chartTopEndpoints.setData(barData);
        
        XAxis xAxis = b.chartTopEndpoints.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setTextSize(9f);
        
        b.chartTopEndpoints.invalidate();
    }

    private void updateRecentErrors() {
        if (statistics.recentErrors == null || statistics.recentErrors.isEmpty()) {
            b.cardRecentErrors.setVisibility(View.GONE);
            return;
        }
        
        b.cardRecentErrors.setVisibility(View.VISIBLE);
        b.layoutRecentErrors.removeAllViews();
        
        int count = Math.min(5, statistics.recentErrors.size());
        for (int i = 0; i < count; i++) {
            ResponseModels.RecentError error = statistics.recentErrors.get(i);
            
            View errorView = getLayoutInflater().inflate(R.layout.item_recent_error, b.layoutRecentErrors, false);
            
            TextView tvErrorType = errorView.findViewById(R.id.tvErrorType);
            TextView tvErrorMessage = errorView.findViewById(R.id.tvErrorMessage);
            TextView tvErrorPath = errorView.findViewById(R.id.tvErrorPath);
            TextView tvErrorTime = errorView.findViewById(R.id.tvErrorTime);
            
            tvErrorType.setText(error.errorType != null ? error.errorType : "Error");
            tvErrorMessage.setText(error.error != null ? error.error : "");
            
            if (error.path != null && !error.path.isEmpty()) {
                tvErrorPath.setText("Path: " + error.path);
                tvErrorPath.setVisibility(View.VISIBLE);
            } else {
                tvErrorPath.setVisibility(View.GONE);
            }
            
            if (error.timestamp != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                    Date date = new Date(error.timestamp);
                    tvErrorTime.setText(sdf.format(date));
                } catch (Exception e) {
                    tvErrorTime.setText(error.timestamp);
                }
            }
            
            b.layoutRecentErrors.addView(errorView);
        }
    }

    private void showSettingsDialog() {
        if (settings == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_log_settings, null);
        builder.setView(dialogView);

        androidx.appcompat.widget.SwitchCompat switchFileLogging = dialogView.findViewById(R.id.switchFileLogging);
        androidx.appcompat.widget.SwitchCompat switchConsoleLogging = dialogView.findViewById(R.id.switchConsoleLogging);

        switchFileLogging.setChecked(settings.enabled);
        switchConsoleLogging.setChecked(settings.consoleEnabled);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            Map<String, Boolean> updates = new HashMap<>();
            updates.put("enabled", switchFileLogging.isChecked());
            updates.put("console_enabled", switchConsoleLogging.isChecked());
            
            updateSettings(updates);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSettings(Map<String, Boolean> updates) {
        ApiClient.get().updateLogSettings(updates).enqueue(new Callback<ResponseModels.LogSettingsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.LogSettingsResponse> call, Response<ResponseModels.LogSettingsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    settings = response.body().settings;
                    updateLoggingStatus();
                    Toast.makeText(SystemLogsActivity.this, response.body().message, Toast.LENGTH_LONG).show();
                    loadStatistics();
                    loadLogs();
                } else {
                    Toast.makeText(SystemLogsActivity.this, "Failed to update settings", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.LogSettingsResponse> call, Throwable t) {
                Toast.makeText(SystemLogsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showClearConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Clear All Logs?")
            .setMessage("This action cannot be undone. Are you sure you want to delete all log entries?")
            .setPositiveButton("Clear Logs", (dialog, which) -> clearLogs())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void clearLogs() {
        ApiClient.get().clearLogs().enqueue(new Callback<com.medreport.ai.models.ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<com.medreport.ai.models.ApiResponse<Void>> call, Response<com.medreport.ai.models.ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(SystemLogsActivity.this, "Logs cleared successfully", Toast.LENGTH_SHORT).show();
                    loadStatistics();
                    loadLogs();
                } else {
                    Toast.makeText(SystemLogsActivity.this, "Failed to clear logs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.medreport.ai.models.ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(SystemLogsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadLogs() {
        ApiClient.get().downloadLogs().enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File file = new File(getExternalFilesDir(null), "app.log");
                        FileOutputStream fos = new FileOutputStream(file);
                        InputStream is = response.body().byteStream();
                        
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        
                        fos.close();
                        is.close();
                        
                        // Share the file
                        Uri fileUri = FileProvider.getUriForFile(SystemLogsActivity.this,
                            getPackageName() + ".provider", file);
                        
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, "Share log file"));
                        
                        Toast.makeText(SystemLogsActivity.this, "Log file downloaded", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(SystemLogsActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SystemLogsActivity.this, "Failed to download logs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(SystemLogsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
