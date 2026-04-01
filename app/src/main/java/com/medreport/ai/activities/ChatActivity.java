package com.medreport.ai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medreport.ai.adapters.MessageAdapter;
import com.medreport.ai.databinding.ActivityChatBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.utils.AuthManager;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.net.URISyntaxException;
import java.util.*;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_REPORT_ID = "report_id";
    public static final String EXTRA_CHAT_TITLE = "chat_title";

    private ActivityChatBinding b;
    private MessageAdapter adapter;
    private final List<MessageModel> messages = new ArrayList<>();
    private String chatId;
    private Socket socket;
    private String myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        String reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        String title    = getIntent().getStringExtra(EXTRA_CHAT_TITLE);
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "Chat");
        }

        UserModel me = AuthManager.getInstance().getCurrentUser();
        myUserId = me != null ? me.id : "";

        adapter = new MessageAdapter(messages, myUserId);
        b.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerView.setAdapter(adapter);

        b.btnSend.setOnClickListener(v -> sendMessage());
        loadChat(reportId);
    }

    private void loadChat(String reportId) {
        b.progressBar.setVisibility(View.VISIBLE);
        ApiClient.get().getChatByReport(reportId).enqueue(new Callback<ResponseModels.ChatResponse>() {
            @Override public void onResponse(Call<ResponseModels.ChatResponse> c, Response<ResponseModels.ChatResponse> r) {
                b.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    chatId = r.body().chat.id;
                    if (r.body().messages != null) {
                        messages.addAll(r.body().messages);
                        adapter.notifyDataSetChanged();
                        scrollToBottom();
                    }
                    connectSocket();
                }
            }
            @Override public void onFailure(Call<ResponseModels.ChatResponse> c, Throwable t) { b.progressBar.setVisibility(View.GONE); }
        });
    }

    private void connectSocket() {
        try {
            IO.Options opts = new IO.Options();
            opts.auth = Collections.singletonMap("token", AuthManager.getInstance().getCachedToken());
            socket = IO.socket("http://10.0.2.2:8081", opts);

            socket.on(Socket.EVENT_CONNECT, args -> {
                JSONObject joinData = new JSONObject();
                try { joinData.put("chat_id", chatId); } catch (Exception ignored) {}
                socket.emit("join_chat", joinData);
            });

            socket.on("new_message", args -> {
                try {
                    JSONObject obj = (JSONObject) args[0];
                    MessageModel msg = new MessageModel();
                    msg.id        = obj.optString("id");
                    msg.senderId  = obj.optString("sender_id");
                    msg.senderRole = obj.optString("sender_role");
                    msg.content   = obj.optString("content");
                    msg.messageType = obj.optString("message_type", "TEXT");
                    msg.timestamp = obj.optString("timestamp");
                    msg.senderName = obj.optString("sender_name");
                    runOnUiThread(() -> {
                        messages.add(msg);
                        adapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                } catch (Exception ignored) {}
            });

            socket.on("user_typing", args -> {
                try {
                    JSONObject obj = (JSONObject) args[0];
                    boolean typing = obj.optBoolean("is_typing");
                    String name = obj.optString("user_name");
                    runOnUiThread(() -> b.tvTyping.setVisibility(typing ? View.VISIBLE : View.GONE));
                } catch (Exception ignored) {}
            });

            socket.connect();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    private void sendMessage() {
        String text = b.etMessage.getText().toString().trim();
        if (text.isEmpty() || chatId == null) return;
        b.etMessage.setText("");

        Map<String, String> body = new HashMap<>();
        body.put("message", text);
        body.put("message_type", "TEXT");

        ApiClient.get().sendMessage(chatId, body).enqueue(new Callback<ApiResponse<MessageModel>>() {
            @Override public void onResponse(Call<ApiResponse<MessageModel>> c, Response<ApiResponse<MessageModel>> r) {}
            @Override public void onFailure(Call<ApiResponse<MessageModel>> c, Throwable t) {
                Toast.makeText(ChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) b.recyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (socket != null) { socket.disconnect(); socket.off(); }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
