package com.medreport.ai.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.models.SystemLog;
import java.util.ArrayList;
import java.util.List;

public class SystemLogAdapter extends RecyclerView.Adapter<SystemLogAdapter.ViewHolder> {

    private List<SystemLog> logs = new ArrayList<>();

    public void setLogs(List<SystemLog> logs) {
        this.logs = logs != null ? logs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_system_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogLevel, tvTimestamp, tvAction, tvMessage, tvUser, tvIpAddress;

        ViewHolder(View itemView) {
            super(itemView);
            tvLogLevel = itemView.findViewById(R.id.tvLogLevel);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvAction = itemView.findViewById(R.id.tvAction);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvIpAddress = itemView.findViewById(R.id.tvIpAddress);
        }

        void bind(SystemLog log) {
            tvLogLevel.setText(log.level != null ? log.level : "INFO");
            tvTimestamp.setText(log.timestamp != null ? log.timestamp : "");
            tvAction.setText(log.type != null ? log.type : "Unknown");
            tvMessage.setText(log.message != null ? log.message : "");
            
            String userEmail = log.getUserEmail();
            String userText = "User: " + (userEmail != null ? userEmail : "System");
            tvUser.setText(userText);
            
            String ipAddress = log.getIpAddress();
            String ipText = "IP: " + (ipAddress != null ? ipAddress : "N/A");
            tvIpAddress.setText(ipText);
            
            // Set color based on log level
            int levelColor = getLevelColor(log.level);
            tvLogLevel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(levelColor));
        }

        private int getLevelColor(String level) {
            if (level == null) return Color.parseColor("#3B82F6"); // Blue for INFO
            
            switch (level.toUpperCase()) {
                case "ERROR":
                case "CRITICAL":
                    return Color.parseColor("#DC2626"); // Red
                case "WARNING":
                    return Color.parseColor("#F59E0B"); // Orange
                case "SUCCESS":
                    return Color.parseColor("#10B981"); // Green
                default:
                    return Color.parseColor("#3B82F6"); // Blue for INFO
            }
        }
    }
}
