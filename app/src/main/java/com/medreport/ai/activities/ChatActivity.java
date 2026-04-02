package com.medreport.ai.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.ByteArrayOutputStream;
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
    private String selectedImageBase64 = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

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
        b.btnAttach.setOnClickListener(v -> openImagePicker());
        b.btnRemoveImage.setOnClickListener(v -> clearImagePreview());

        setupImagePicker();
        loadChat(reportId);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    Bitmap bitmap;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    }
                    // Compress and convert to Base64
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
                    byte[] byteArray = outputStream.toByteArray();
                    selectedImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);

                    // Show preview
                    b.cvImagePreview.setVisibility(View.VISIBLE);
                    b.ivPreview.setImageURI(uri);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void clearImagePreview() {
        selectedImageBase64 = null;
        b.cvImagePreview.setVisibility(View.GONE);
        b.ivPreview.setImageDrawable(null);
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
                    msg.imageData  = obj.optString("image_data", null);
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
        if ((text.isEmpty() && selectedImageBase64 == null) || chatId == null) return;
        b.etMessage.setText("");

        Map<String, String> body = new HashMap<>();
        if (!text.isEmpty()) body.put("message", text);
        
        if (selectedImageBase64 != null) {
            body.put("message_type", "IMAGE");
            body.put("image_data", selectedImageBase64);
            body.put("file_name", "image_" + System.currentTimeMillis() + ".jpg");
        } else {
            body.put("message_type", "TEXT");
        }
        
        clearImagePreview();

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

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.medreport.ai.R.menu.menu_chat, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == com.medreport.ai.R.id.action_delete) {
            confirmDeleteChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteChat() {
        if (chatId == null) return;
        new AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this conversation? This cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteChat())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteChat() {
        b.progressBar.setVisibility(View.VISIBLE);
        ApiClient.get().deleteChat(chatId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                b.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Chat deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to delete chat", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
