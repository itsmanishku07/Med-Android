package com.medreport.ai.fragments;

import com.medreport.ai.R;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.medreport.ai.adapters.AIChatAdapter;
import com.medreport.ai.databinding.BottomSheetAiChatBinding;
import com.medreport.ai.models.AIChatMessage;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.network.ApiClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public class AIChatBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "AIChatBottomSheet";
    private static final String ARG_REPORT_ID = "report_id";
    private BottomSheetAiChatBinding b;
    private String reportId;
    private AIChatAdapter adapter;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;

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
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            parent.setBackgroundResource(android.R.color.transparent);
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            
            ViewGroup.LayoutParams layoutParams = parent.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            parent.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupChat();
        loadHistory();
        setupSpeechRecognizer();
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                b.etQuestion.setHint("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                b.btnMic.setIconResource(R.drawable.ic_mic);
                b.etQuestion.setHint("Type your question...");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                b.btnMic.setIconResource(R.drawable.ic_mic);
                b.etQuestion.setHint("Type your question...");
                Log.e(TAG, "Speech Error: " + error);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null && !data.isEmpty()) {
                    b.etQuestion.setText(data.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null && !data.isEmpty()) {
                    b.etQuestion.setText(data.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void updateClearButtonVisibility() {
        b.btnClear.setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
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

        b.btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            } else {
                toggleListening();
            }
        });

        b.btnClear.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to clear your AI chat history? This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearHistory())
                .setNegativeButton("Cancel", null)
                .show();
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
                    updateClearButtonVisibility();
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
        AIChatMessage userMsg = new AIChatMessage("user", question);
        adapter.addMessage(userMsg);
        updateClearButtonVisibility();
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

    private void clearHistory() {
        b.progressLoading.setVisibility(View.VISIBLE);
        ApiClient.get().deleteAIChatHistory(reportId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                b.progressLoading.setVisibility(View.GONE);
                if (r.isSuccessful()) {
                    adapter.setMessages(null);
                    updateClearButtonVisibility();
                    Toast.makeText(getContext(), "Chat history cleared", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to clear history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                b.progressLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleListening() {
        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            b.btnMic.setIconResource(R.drawable.ic_mic);
        } else {
            speechRecognizer.startListening(speechRecognizerIntent);
            isListening = true;
            b.btnMic.setIconResource(R.drawable.ic_stop);
        }
    }

    @Override
    public void onDestroyView() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroyView();
        b = null;
    }
}
