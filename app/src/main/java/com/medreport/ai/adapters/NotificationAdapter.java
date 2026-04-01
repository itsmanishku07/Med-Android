package com.medreport.ai.adapters;

import android.graphics.Typeface;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.models.NotificationModel;
import com.medreport.ai.utils.DateUtils;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    public interface MarkReadCallback { void markRead(String id); }
    private final List<NotificationModel> items;
    private final MarkReadCallback cb;

    public NotificationAdapter(List<NotificationModel> items, MarkReadCallback cb) { this.items = items; this.cb = cb; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        NotificationModel n = items.get(pos);
        h.tvTitle.setText(n.title);
        h.tvMessage.setText(n.message);
        h.tvTime.setText(DateUtils.timeAgo(n.createdAt));
        int style = n.read ? Typeface.NORMAL : Typeface.BOLD;
        h.tvTitle.setTypeface(null, style);
        h.itemView.setAlpha(n.read ? 0.7f : 1f);
        h.itemView.setOnClickListener(v -> { if (!n.read) cb.markRead(n.id); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        VH(View v) {
            super(v);
            tvTitle   = v.findViewById(R.id.tvTitle);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime    = v.findViewById(R.id.tvTime);
        }
    }
}
