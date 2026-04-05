package com.medreport.ai.adapters;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.databinding.ItemAiChatMessageBinding;
import com.medreport.ai.models.AIChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;

public class AIChatAdapter extends RecyclerView.Adapter<AIChatAdapter.ChatViewHolder> {

    private final List<AIChatMessage> messages = new ArrayList<>();

    public void setMessages(List<AIChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void addMessage(AIChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAiChatMessageBinding b = ItemAiChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ChatViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemAiChatMessageBinding b;
        private final SimpleDateFormat displayFmt = new SimpleDateFormat("HH:mm", Locale.US);
        private final SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        private final Markwon markwon;

        ChatViewHolder(ItemAiChatMessageBinding binding) {
            super(binding.getRoot());
            this.b = binding;
            this.markwon = Markwon.builder(binding.getRoot().getContext())
                    .usePlugin(TablePlugin.create(binding.getRoot().getContext()))
                    .build();
        }

        void bind(AIChatMessage msg) {
            boolean isUser = "user".equalsIgnoreCase(msg.role);
            b.layoutSent.setVisibility(isUser ? View.VISIBLE : View.GONE);
            b.layoutReceived.setVisibility(isUser ? View.GONE : View.VISIBLE);

            String timeStr = "";
            try {
                Date date = isoFmt.parse(msg.timestamp);
                if (date != null) timeStr = displayFmt.format(date);
            } catch (Exception ignored) { timeStr = msg.timestamp; }

            if (isUser) {
                b.tvSentContent.setText(msg.content);
                b.tvSentTime.setText(timeStr);
            } else {
                // Render Markdown for AI responses using Markwon
                markwon.setMarkdown(b.tvReceivedContent, msg.content);
                b.tvReceivedTime.setText(timeStr);
            }
        }
    }
}
