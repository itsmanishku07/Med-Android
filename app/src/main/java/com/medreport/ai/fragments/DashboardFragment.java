package com.medreport.ai.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.medreport.ai.R;
import com.medreport.ai.activities.ChatActivity;
import com.medreport.ai.activities.ReportDashboardAdapter;
import com.medreport.ai.databinding.FragmentDashboardBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.utils.AuthManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment implements ReportDashboardAdapter.OnItemInteractionListener {
    
    private FragmentDashboardBinding b;
    private UserModel user;
    private ReportDashboardAdapter adapter;
    private List<ReportModel> fullReportList = new ArrayList<>();
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean isPolling = false;

    // Doctor filters
    private String viewMode = "ACTIVE"; // ACTIVE or ARCHIVED
    private String priorityFilter = "ALL";

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) uploadReport(uri);
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        b = FragmentDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        user = AuthManager.getInstance().getCurrentUser();
        if (user == null) return;

        setupUI();
        setupListeners();

        loadStats();
        loadReports();
    }

    private void setupUI() {
        String firstName = (user.name != null && !user.name.trim().isEmpty()) ? user.name.split(" ")[0] : "User";
        if (user.isDoctor()) {
            b.tvWelcomeHeader.setText("Doctor Dashboard");
            b.tvWelcomeSub.setText("Dr. " + firstName);
            b.cardUpload.setVisibility(View.GONE);
            b.layoutDoctorFilters.setVisibility(View.VISIBLE);
            b.tvListHeader.setText("Patient Assignments");
            
            b.tvStat1Label.setText("TOTAL PATIENTS");
            b.tvStat2Label.setText("PENDING REVIEWS");
            b.tvStat3Label.setText("COMPLETED TODAY");
            b.tvStat4Label.setText("CRITICAL CASES");
        } else {
            b.tvWelcomeHeader.setText("Patient Dashboard");
            b.tvWelcomeSub.setText("Welcome back, " + firstName);
            b.cardUpload.setVisibility(View.VISIBLE);
            b.layoutDoctorFilters.setVisibility(View.GONE);
            b.tvListHeader.setText("My Medical Reports");

            b.tvStat1Label.setText("TOTAL REPORTS");
            b.tvStat2Label.setText("PENDING REVIEW");
            b.tvStat3Label.setText("REVIEWED");
            b.tvStat4Label.setText("CRITICAL ALERTS");
        }
        
        adapter = new ReportDashboardAdapter(requireContext(), new ArrayList<>(), user.isDoctor() || user.isAdmin(), this);
        b.rvDashboardItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvDashboardItems.setAdapter(adapter);
    }

    private void setupListeners() {
        b.btnUploadBox.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        b.toggleViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnActive) {
                    viewMode = "ACTIVE";
                    b.cgPriorityFilters.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.btnArchived) {
                    viewMode = "ARCHIVED";
                    b.cgPriorityFilters.setVisibility(View.GONE);
                }
                filterList();
            }
        });

        b.cgPriorityFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterAll) priorityFilter = "ALL";
            else if (checkedId == R.id.chipFilterCritical) priorityFilter = "CRITICAL";
            else if (checkedId == R.id.chipFilterHigh) priorityFilter = "HIGH";
            else if (checkedId == R.id.chipFilterPending) priorityFilter = "PENDING";
            filterList();
        });
    }

    private void startPolling() {
        if (isPolling) return;
        isPolling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                loadReportsSilently();
                loadStats();
                if (isPolling) handler.postDelayed(this, 5000);
            }
        };
        handler.post(pollRunnable);
    }

    private void stopPolling() {
        isPolling = false;
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }

    private void loadStats() {
        ApiClient.get().getStats().enqueue(new Callback<ResponseModels.StatsResponse>() {
            @Override public void onResponse(Call<ResponseModels.StatsResponse> call, Response<ResponseModels.StatsResponse> r) {
                if (!isAdded() || getContext() == null) return;
                if (r.isSuccessful() && r.body() != null && r.body().stats != null) {
                    Map<String, Object> s = r.body().stats;
                    b.tvStat1Value.setText(String.valueOf(toNumber(s.get("totalReports"))));
                    b.tvStat2Value.setText(String.valueOf(toNumber(s.get("pendingReports"))));
                    b.tvStat3Value.setText(String.valueOf(toNumber(s.get("reviewedReports"))));
                    b.tvStat4Value.setText(String.valueOf(toNumber(s.get("criticalAlerts"))));
                }
            }
            @Override public void onFailure(Call<ResponseModels.StatsResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
            }
        });
    }

    private void loadReports() {
        b.layoutSkeleton.getRoot().setVisibility(View.VISIBLE);
        b.rvDashboardItems.setVisibility(View.GONE);
        loadReportsSilently();
    }

    private void loadReportsSilently() {
        ApiClient.get().getMyReports().enqueue(new Callback<ResponseModels.ReportsResponse>() {
            @Override public void onResponse(Call<ResponseModels.ReportsResponse> call, Response<ResponseModels.ReportsResponse> r) {
                if (!isAdded() || getContext() == null) return;
                
                b.layoutSkeleton.getRoot().setVisibility(View.GONE);
                b.rvDashboardItems.setVisibility(View.VISIBLE);
                
                if (r.isSuccessful() && r.body() != null && r.body().reports != null) {
                    fullReportList = r.body().reports;
                    filterList();
                    
                    // Simple fade animation for content appearance
                    b.rvDashboardItems.setAlpha(0f);
                    b.rvDashboardItems.animate().alpha(1f).setDuration(400).start();
                    
                    checkIfPollingNeeded();
                } else {
                    fullReportList.clear();
                    filterList();
                }
            }
            @Override public void onFailure(Call<ResponseModels.ReportsResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                
                b.layoutSkeleton.getRoot().setVisibility(View.GONE);
                b.rvDashboardItems.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filterList() {
        List<ReportModel> filtered = new ArrayList<>();
        if (!user.isDoctor() && !user.isAdmin()) {
            filtered.addAll(fullReportList);
        } else {
            for (ReportModel r : fullReportList) {
                if (viewMode.equals("ARCHIVED")) {
                    if (r.isArchived) filtered.add(r);
                } else {
                    if (r.isArchived) continue;
                    if (priorityFilter.equals("ALL")) {
                        filtered.add(r);
                    } else if (priorityFilter.equals("PENDING")) {
                        if ("PENDING".equals(r.status)) filtered.add(r);
                    } else {
                        if (priorityFilter.equals(r.getSeverityLevel())) filtered.add(r);
                    }
                }
            }
        }
        
        b.layoutEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        adapter = new ReportDashboardAdapter(requireContext(), filtered, user.isDoctor() || user.isAdmin(), this);
        b.rvDashboardItems.setAdapter(adapter);
    }

    private void checkIfPollingNeeded() {
        boolean needsPolling = false;
        for (ReportModel r : fullReportList) {
            if ("ANALYZING".equals(r.status) || "PENDING".equals(r.status)) {
                needsPolling = true;
                break;
            }
        }
        if (needsPolling && !isPolling) {
            startPolling();
        } else if (!needsPolling && isPolling) {
            Toast.makeText(getContext(), "Analysis completed!", Toast.LENGTH_SHORT).show();
            stopPolling();
        }
    }

    private void uploadReport(Uri uri) {
        b.uploadProgress.setVisibility(View.VISIBLE);
        try {
            String fileName = getFileName(uri);
            if (fileName == null) fileName = "uploaded_report.pdf";

            android.util.Log.d("DashboardFragment", "Starting upload for file: " + fileName);

            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            File f = new File(requireContext().getCacheDir(), "upload_temp");
            FileOutputStream fos = new FileOutputStream(f);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            is.close();
            fos.close();

            android.util.Log.d("DashboardFragment", "File size: " + f.length() + " bytes");

            String mimeType = requireContext().getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "application/octet-stream";
            
            android.util.Log.d("DashboardFragment", "MIME type: " + mimeType);

            RequestBody reqFile = RequestBody.create(MediaType.parse(mimeType), f);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, reqFile);
            RequestBody type = RequestBody.create(MediaType.parse("text/plain"), "GENERAL");

            ApiClient.get().uploadReport(body, type).enqueue(new Callback<ApiResponse<ReportModel>>() {
                @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                    b.uploadProgress.setVisibility(View.GONE);
                    android.util.Log.d("DashboardFragment", "Upload response code: " + r.code());
                    
                    if (r.isSuccessful()) {
                        Toast.makeText(getContext(), "Report uploaded successfully!", Toast.LENGTH_SHORT).show();
                        loadReports();
                        loadStats();
                    } else {
                        String errorMsg = "Upload failed";
                        try {
                            if (r.errorBody() != null) {
                                errorMsg = r.errorBody().string();
                                android.util.Log.e("DashboardFragment", "Error body: " + errorMsg);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("DashboardFragment", "Error reading error body", e);
                        }
                        Toast.makeText(getContext(), "Upload failed: " + r.code(), Toast.LENGTH_LONG).show();
                    }
                }
                @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {
                    b.uploadProgress.setVisibility(View.GONE);
                    android.util.Log.e("DashboardFragment", "Upload failed", t);
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            b.uploadProgress.setVisibility(View.GONE);
            android.util.Log.e("DashboardFragment", "Error preparing file", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private int toNumber(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) try { return (int) Double.parseDouble((String) o); } catch (Exception ignored) {}
        return 0;
    }

    // -- Adapter Listeners --
    @Override public void onConsultDoctor(ReportModel r) {
        Intent i = new Intent(requireContext(), ChatActivity.class);
        i.putExtra("report_id", r.id);
        startActivity(i);
    }
    
    @Override public void onChatPatient(ReportModel r) {
        Intent i = new Intent(requireContext(), ChatActivity.class);
        i.putExtra("report_id", r.id);
        startActivity(i);
    }

    @Override public void onDelete(ReportModel r) {
        ApiClient.get().deleteReport(r.id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) { loadReports(); loadStats(); }
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });
    }

    @Override public void onAcceptCase(ReportModel r) {
        Map<String, String> body = new HashMap<>();
        body.put("doctor_id", user.firebaseUid);
        ApiClient.get().assignDoctor(r.id, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) { loadReports(); }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {}
        });
    }

    @Override public void onToggleArchive(ReportModel r) {
        Map<String, Boolean> body = new HashMap<>();
        body.put("is_archived", !r.isArchived);
        ApiClient.get().archiveReport(r.id, body).enqueue(new Callback<ApiResponse<ReportModel>>() {
            @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) { loadReports(); }
            @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) {}
        });
    }
}
