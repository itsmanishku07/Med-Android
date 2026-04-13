package com.medreport.ai.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.*;
import android.widget.ImageView;
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
    public MessageAdapter(List<MessageModel> items, String myUserId) {
        this.items = items;
        this.myUserId = myUserId;
    }

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
        if (m.content != null && !m.content.isEmpty()) {
            h.tvMessage.setVisibility(View.VISIBLE);
            h.markwon.setMarkdown(h.tvMessage, m.content);
        } else {
            h.tvMessage.setVisibility(View.GONE);
        }
        h.tvTime.setText(DateUtils.timeAgo(m.timestamp));

        if ("IMAGE".equals(m.messageType) && m.imageData != null && m.imageData.startsWith("data:image")) {
            h.ivImage.setVisibility(View.VISIBLE);
            try {
                String base64Image = m.imageData.split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                h.ivImage.setImageBitmap(decodedByte);
            } catch (Exception e) {
                e.printStackTrace();
                h.ivImage.setVisibility(View.GONE);
            }
        } else {
            h.ivImage.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;
        io.noties.markwon.Markwon markwon;
        VH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime    = v.findViewById(R.id.tvTime);
            ivImage   = v.findViewById(R.id.ivImage);
            markwon   = io.noties.markwon.Markwon.create(v.getContext());
        }
    }
}
