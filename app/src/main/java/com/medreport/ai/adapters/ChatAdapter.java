package com.medreport.ai.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.models.ChatModel;
import com.medreport.ai.utils.DateUtils;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
    public interface Listener { void onChatClick(ChatModel c); }
    private final List<ChatModel> items;
    private final Listener listener;

    public ChatAdapter(List<ChatModel> items, Listener listener) { this.items = items; this.listener = listener; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ChatModel c = items.get(pos);
        h.tvTitle.setText(c.reportName != null ? c.reportName : "Chat");
        h.tvSubtitle.setText("Patient: " + (c.patientName != null ? c.patientName : "—") + " · Dr. " + (c.doctorName != null ? c.doctorName : "—"));
        h.tvTime.setText(DateUtils.timeAgo(c.lastMessageAt));
        h.tvUnread.setVisibility(c.unreadCount > 0 ? View.VISIBLE : View.GONE);
        h.tvUnread.setText(String.valueOf(c.unreadCount));
        h.itemView.setOnClickListener(v -> listener.onChatClick(c));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvTime, tvUnread;
        VH(View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tvTitle);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            tvTime     = v.findViewById(R.id.tvTime);
            tvUnread   = v.findViewById(R.id.tvUnread);
        }
    }
}
