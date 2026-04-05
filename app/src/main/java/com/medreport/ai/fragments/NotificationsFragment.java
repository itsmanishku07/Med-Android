package com.medreport.ai.fragments;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medreport.ai.adapters.NotificationAdapter;
import com.medreport.ai.databinding.FragmentNotificationsBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private FragmentNotificationsBinding b;
    private NotificationAdapter adapter;
    private final List<NotificationModel> notifs = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        b = FragmentNotificationsBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new NotificationAdapter(notifs, this::markRead);
        b.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recyclerView.setAdapter(adapter);
        b.swipeRefresh.setOnRefreshListener(this::loadNotifs);
        b.btnMarkAll.setOnClickListener(v -> markAllRead());
        loadNotifs();
    }

    private void loadNotifs() {
        b.swipeRefresh.setRefreshing(true);
        ApiClient.get().getNotifications(false, 50).enqueue(new Callback<ResponseModels.NotificationsResponse>() {
            @Override public void onResponse(Call<ResponseModels.NotificationsResponse> c, Response<ResponseModels.NotificationsResponse> r) {
                b.swipeRefresh.setRefreshing(false);
                if (r.isSuccessful() && r.body() != null && r.body().notifications != null) {
                    notifs.clear(); notifs.addAll(r.body().notifications);
                    adapter.notifyDataSetChanged();
                    b.tvEmpty.setVisibility(notifs.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<ResponseModels.NotificationsResponse> c, Throwable t) { b.swipeRefresh.setRefreshing(false); }
        });
    }

    private void markRead(String id) {
        ApiClient.get().markRead(id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                for (NotificationModel n : notifs) if (n.id.equals(id)) { n.read = true; break; }
                adapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });
    }

    private void markAllRead() {
        ApiClient.get().markAllRead().enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                for (NotificationModel n : notifs) n.read = true;
                adapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });
    }
}
