package com.medreport.ai.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.medreport.ai.adapters.AIChatAdapter;
import com.medreport.ai.databinding.BottomSheetAiChatBinding;
import com.medreport.ai.models.AIChatMessage;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.network.ApiClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "AIChatBottomSheet";
    private static final String ARG_REPORT_ID = "report_id";
    private BottomSheetAiChatBinding b;
    private String reportId;
    private AIChatAdapter adapter;

    public static AIChatBottomSheet newInstance(String reportId) {
        AIChatBottomSheet fragment = new AIChatBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reportId = getArguments().getString(ARG_REPORT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = BottomSheetAiChatBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupChat();
        loadHistory();
    }

    private void setupChat() {
        adapter = new AIChatAdapter();
        b.rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvChat.setAdapter(adapter);

        b.btnSend.setOnClickListener(v -> {
            String q = b.etQuestion.getText().toString().trim();
            if (!q.isEmpty()) {
                sendQuestion(q);
            }
        });
    }

    private void loadHistory() {
        b.progressLoading.setVisibility(View.VISIBLE);
        ApiClient.get().getAIChatHistory(reportId).enqueue(new Callback<ResponseModels.AIChatResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.AIChatResponse> call, Response<ResponseModels.AIChatResponse> r) {
                b.progressLoading.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null && r.body().success) {
                    adapter.setMessages(r.body().history);
                    if (adapter.getItemCount() > 0) {
                        b.rvChat.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.AIChatResponse> call, Throwable t) {
                b.progressLoading.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load chat history", t);
            }
        });
    }

    private void sendQuestion(String question) {
        // Optimistic UI update
        AIChatMessage userMsg = new AIChatMessage("user", question);
        adapter.addMessage(userMsg);
        b.rvChat.scrollToPosition(adapter.getItemCount() - 1);
        b.etQuestion.setText("");
        
        b.btnSend.setEnabled(false);
        b.progressLoading.setVisibility(View.VISIBLE);

        Map<String, String> body = new HashMap<>();
        body.put("question", question);

        ApiClient.get().askAIQuestion(reportId, body).enqueue(new Callback<ResponseModels.AIAskResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.AIAskResponse> call, Response<ResponseModels.AIAskResponse> r) {
                b.btnSend.setEnabled(true);
                b.progressLoading.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null && r.body().success) {
                    // answer is the AI reply; question is already shown optimistically
                    if (r.body().answer != null) {
                        adapter.addMessage(r.body().answer);
                        b.rvChat.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                } else {
                    Toast.makeText(getContext(), "AI missed that one. Try again?", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.AIAskResponse> call, Throwable t) {
                b.btnSend.setEnabled(true);
                b.progressLoading.setVisibility(View.GONE);
                Log.e(TAG, "Question failed", t);
                Toast.makeText(getContext(), "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
