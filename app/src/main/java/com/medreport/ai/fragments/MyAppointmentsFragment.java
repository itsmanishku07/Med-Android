package com.medreport.ai.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.medreport.ai.adapters.AppointmentAdapter;
import com.medreport.ai.databinding.FragmentMyAppointmentsBinding;
import com.medreport.ai.models.AppointmentModel;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyAppointmentsFragment extends Fragment implements AppointmentAdapter.OnAppointmentInteractionListener {

    private FragmentMyAppointmentsBinding b;
    private List<AppointmentModel> fullList = new ArrayList<>();
    private AppointmentAdapter adapter;
    private String currentTab = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        b = FragmentMyAppointmentsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupUI();
        loadAppointments();
    }

    private void setupUI() {
        b.rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppointmentAdapter(requireContext(), new ArrayList<>(), false, this);
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
        ApiClient.get().getPatientAppointments().enqueue(new Callback<ResponseModels.AppointmentsResponse>() {
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

        b.layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        adapter = new AppointmentAdapter(requireContext(), filtered, false, this);
        b.rvAppointments.setAdapter(adapter);
    }

    @Override
    public void onManage(AppointmentModel appointment) {
    }

    @Override
    public void onItemClicked(AppointmentModel appointment) {
    }
}
