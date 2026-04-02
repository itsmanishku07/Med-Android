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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // ── Markdown helpers ───────────────────────────────────────────────────────

    /** Converts a Markdown-lite string (bold + bullets + numbered lists) to SpannableStringBuilder. */
    private static SpannableStringBuilder renderMarkdown(String raw) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String[] lines = raw.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                continue;
            }

            boolean isBullet  = trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ");
            boolean isNumered = trimmed.matches("^\\d+\\.\\s.*");

            String content = trimmed;
            if (isBullet)  content = trimmed.substring(2);
            else if (isNumered) content = trimmed.replaceFirst("^\\d+\\.\\s", "");

            int start = sb.length();
            appendBoldMarkdown(sb, content);

            if (isBullet) {
                sb.setSpan(new BulletSpan(16), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < lines.length - 1) sb.append("\n");
        }
        return sb;
    }

    /** Renders **bold** segments inside a line. */
    private static void appendBoldMarkdown(SpannableStringBuilder sb, String text) {
        Pattern p = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher m = p.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) sb.append(text, last, m.start());
            int boldStart = sb.length();
            sb.append(m.group(1));
            sb.setSpan(new StyleSpan(Typeface.BOLD), boldStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            last = m.end();
        }
        if (last < text.length()) sb.append(text, last, text.length());
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemAiChatMessageBinding b;
        private final SimpleDateFormat displayFmt = new SimpleDateFormat("HH:mm", Locale.US);
        private final SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        ChatViewHolder(ItemAiChatMessageBinding binding) {
            super(binding.getRoot());
            this.b = binding;
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
                // Render Markdown for AI responses
                b.tvReceivedContent.setText(renderMarkdown(msg.content));
                b.tvReceivedTime.setText(timeStr);
            }
        }
    }
}
