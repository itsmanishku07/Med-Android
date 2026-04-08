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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.medreport.ai.R;
import com.medreport.ai.activities.AdminDashboardActivity;
import com.medreport.ai.adapters.AdminUserAdapter;
import com.medreport.ai.adapters.ReportAdapter;
import com.medreport.ai.adapters.SystemLogAdapter;
import com.medreport.ai.databinding.FragmentAdminDashboardBinding;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.models.SystemLog;
import com.medreport.ai.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding b;
    private AdminUserAdapter userAdapter;
    private ReportAdapter reportAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupAdapters();
        setupTabs();
        setupListeners();
        loadDashboardData();
    }

    private void setupAdapters() {
        // Users adapter
        userAdapter = new AdminUserAdapter(user -> {
            Toast.makeText(getContext(), "User: " + user.name, Toast.LENGTH_SHORT).show();
        });
        b.rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvUsers.setAdapter(userAdapter);

        // Reports adapter
        reportAdapter = new ReportAdapter(new java.util.ArrayList<>(), report -> {
            Toast.makeText(getContext(), "Report: " + report.fileName, Toast.LENGTH_SHORT).show();
        });
        b.rvReports.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvReports.setAdapter(reportAdapter);
    }

    private void setupTabs() {
        b.tabLayout.addTab(b.tabLayout.newTab().setText("Overview"));
        b.tabLayout.addTab(b.tabLayout.newTab().setText("Users"));
        b.tabLayout.addTab(b.tabLayout.newTab().setText("Reports"));
        b.tabLayout.addTab(b.tabLayout.newTab().setText("System Logs"));

        b.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        b.btnDatabaseAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AdminDashboardActivity.class);
            startActivity(intent);
        });
    }

    private void switchTab(int position) {
        b.layoutOverview.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        b.layoutUsers.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        b.layoutReports.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        
        if (position == 1) {
            loadUsers();
        } else if (position == 2) {
            loadReports();
        } else if (position == 3) {
            // Open System Logs Activity
            Intent intent = new Intent(getContext(), com.medreport.ai.activities.SystemLogsActivity.class);
            startActivity(intent);
            // Reset to overview tab
            b.tabLayout.getTabAt(0).select();
        }
    }

    private void loadDashboardData() {
        if (b == null || !isAdded()) return;
        b.progressBar.setVisibility(View.VISIBLE);

        ApiClient.get().getAdminDashboard().enqueue(new Callback<ResponseModels.AdminDashboardResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.AdminDashboardResponse> call, Response<ResponseModels.AdminDashboardResponse> response) {
                if (b == null || !isAdded() || getContext() == null) return;
                b.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Map<String, Object> stats = response.body().stats;
                    updateStats(stats);
                } else {
                    Toast.makeText(getContext(), "Failed to load dashboard data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.AdminDashboardResponse> call, Throwable t) {
                if (b == null || !isAdded() || getContext() == null) return;
                b.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats(Map<String, Object> stats) {
        if (b == null || !isAdded()) return;
        
        int totalUsers = getIntValue(stats.get("totalUsers"));
        int totalDoctors = getIntValue(stats.get("totalDoctors"));
        int totalPatients = getIntValue(stats.get("totalPatients"));
        int totalReports = getIntValue(stats.get("totalReports"));
        int analyzedReports = getIntValue(stats.get("analyzedReports"));
        int criticalCases = getIntValue(stats.get("criticalCases"));
        int activeChats = getIntValue(stats.get("activeChats"));

        b.tvTotalUsers.setText(String.valueOf(totalUsers));
        b.tvUserBreakdown.setText(totalDoctors + " Docs · " + totalPatients + " Pts");
        b.tvTotalReports.setText(String.valueOf(totalReports));
        b.tvAnalyzedReports.setText(analyzedReports + " Analyzed");
        b.tvCriticalCases.setText(String.valueOf(criticalCases));
        b.tvActiveChats.setText(String.valueOf(activeChats));
    }

    private void loadUsers() {
        if (b == null || !isAdded()) return;
        b.progressBar.setVisibility(View.VISIBLE);
        
        ApiClient.get().getAllUsers().enqueue(new Callback<ResponseModels.UsersResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.UsersResponse> call, Response<ResponseModels.UsersResponse> response) {
                if (b == null || !isAdded() || getContext() == null) return;
                b.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    userAdapter.setUsers(response.body().users);
                    if (response.body().users.isEmpty()) {
                        Toast.makeText(getContext(), "No users found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.UsersResponse> call, Throwable t) {
                if (b == null || !isAdded() || getContext() == null) return;
                b.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error loading users: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReports() {
        if (b == null || !isAdded()) return;
        b.progressBar.setVisibility(View.VISIBLE);
        
        ApiClient.get().getMyReports().enqueue(new Callback<ResponseModels.ReportsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.ReportsResponse> call, Response<ResponseModels.ReportsResponse> response) {
                if (b == null || !isAdded() || getContext() == null) return;
                b.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    // Create new adapter with the reports list
                    reportAdapter = new ReportAdapter(response.body().reports, report -> {
                        Toast.makeText(getContext(), "Report: " + report.fileName, Toast.LENGTH_SHORT).show();
                    });
                    b.rvReports.setAdapter(reportAdapter);
                    
                    if (response.body().reports.isEmpty()) {
                        Toast.makeText(getContext(), "No reports found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load reports", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.ReportsResponse> call, Throwable t) {
                if (b == null || !isAdded() || getContext() == null) return;
                b.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error loading reports: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
