package com.medreport.ai.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.models.MessageModel;
import com.medreport.ai.utils.DateUtils;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
    private static final int VIEW_SENT = 0, VIEW_RECV = 1;
    private final List<MessageModel> items;
    private final String myUserId;

    public MessageAdapter(List<MessageModel> items, String myUserId) { this.items = items; this.myUserId = myUserId; }

    @Override public int getItemViewType(int pos) {
        return myUserId.equals(items.get(pos).senderId) ? VIEW_SENT : VIEW_RECV;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_SENT ? R.layout.item_message_sent : R.layout.item_message_recv;
        return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        MessageModel m = items.get(pos);
        h.tvMessage.setText(m.content);
        h.tvTime.setText(DateUtils.timeAgo(m.timestamp));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        VH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime    = v.findViewById(R.id.tvTime);
        }
    }
}
