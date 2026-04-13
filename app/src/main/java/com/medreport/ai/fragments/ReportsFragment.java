package com.medreport.ai.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medreport.ai.R;
import com.medreport.ai.activities.ReportDetailActivity;
import com.medreport.ai.adapters.ReportAdapter;
import com.medreport.ai.databinding.FragmentReportsBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment implements ReportAdapter.Listener {
    private FragmentReportsBinding b;
    private ReportAdapter adapter;
    private final List<ReportModel> reports = new ArrayList<>();
    private ActivityResultLauncher<String[]> filePicker;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) uploadReport(uri);
        });
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        b = FragmentReportsBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new ReportAdapter(reports, this);
        b.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recyclerView.setAdapter(adapter);
        
        UserModel currentUser = com.medreport.ai.utils.AuthManager.getInstance().getCurrentUser();
        if (currentUser != null && "DOCTOR".equals(currentUser.role)) {
            b.fab.setVisibility(View.GONE);
            b.layoutFilters.setVisibility(View.VISIBLE);
            b.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
                loadReports();
            });
        } else {
            b.fab.setOnClickListener(v -> filePicker.launch(new String[]{"application/pdf", "image/*"}));
            b.layoutFilters.setVisibility(View.GONE);
        }
        
        b.swipeRefresh.setOnRefreshListener(this::loadReports);
        loadReports();
    }
    private void loadReports() {
        b.swipeRefresh.setRefreshing(true);
        b.layoutSkeleton.getRoot().setVisibility(View.VISIBLE);
        b.recyclerView.setVisibility(View.GONE);
        b.tvEmpty.setVisibility(View.GONE);

        UserModel user = com.medreport.ai.utils.AuthManager.getInstance().getCurrentUser();
        boolean isPrivateMode = user != null && user.isDoctor() && b.chipPrivate.isChecked();
        
        Call<ResponseModels.ReportsResponse> call = isPrivateMode 
                ? ApiClient.get().getPrivateReports() 
                : ApiClient.get().getMyReports();
        
        call.enqueue(new Callback<ResponseModels.ReportsResponse>() {
            @Override public void onResponse(Call<ResponseModels.ReportsResponse> c, Response<ResponseModels.ReportsResponse> r) {
                b.swipeRefresh.setRefreshing(false);
                b.layoutSkeleton.getRoot().setVisibility(View.GONE);
                b.recyclerView.setVisibility(View.VISIBLE);
                
                if (r.isSuccessful() && r.body() != null && r.body().reports != null) {
                    reports.clear(); 
                    reports.addAll(r.body().reports);
                    adapter.notifyDataSetChanged();
                    
                    android.view.animation.LayoutAnimationController controller = 
                        android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down);
                    b.recyclerView.setLayoutAnimation(controller);
                    b.recyclerView.scheduleLayoutAnimation();
                    
                    b.tvEmpty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<ResponseModels.ReportsResponse> c, Throwable t) { 
                b.swipeRefresh.setRefreshing(false); 
                b.layoutSkeleton.getRoot().setVisibility(View.GONE);
                b.recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void uploadReport(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(is);
            String mime = requireContext().getContentResolver().getType(uri);
            if (mime == null) mime = "application/pdf";
            RequestBody rb = RequestBody.create(bytes, MediaType.parse(mime));
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", "report." + getExt(mime), rb);
            b.swipeRefresh.setRefreshing(true);
        RequestBody type = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), "GENERAL");
        ApiClient.get().uploadReport(part, type).enqueue(new Callback<ApiResponse<ReportModel>>() {
                @Override public void onResponse(Call<ApiResponse<ReportModel>> c, Response<ApiResponse<ReportModel>> r) {
                    b.swipeRefresh.setRefreshing(false);
                    if (r.isSuccessful()) { Toast.makeText(requireContext(), "Uploaded! Analysis started.", Toast.LENGTH_SHORT).show(); loadReports(); }
                }
                @Override public void onFailure(Call<ApiResponse<ReportModel>> c, Throwable t) { b.swipeRefresh.setRefreshing(false); Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show(); }
            });
        } catch (Exception e) { Toast.makeText(requireContext(), "Error reading file", Toast.LENGTH_SHORT).show(); }
    }

    private byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096]; int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private String getExt(String mime) {
        if (mime.contains("pdf")) return "pdf";
        if (mime.contains("png")) return "png";
        return "jpg";
    }

    @Override public void onReportClick(ReportModel r) {
        Intent i = new Intent(requireContext(), ReportDetailActivity.class);
        i.putExtra(ReportDetailActivity.EXTRA_REPORT_ID, r.id);
        startActivity(i);
    }
}
