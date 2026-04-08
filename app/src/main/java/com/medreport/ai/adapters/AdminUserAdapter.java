package com.medreport.ai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.medreport.ai.R;
import com.medreport.ai.models.UserModel;
import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private List<UserModel> users = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(UserModel user);
    }

    public AdminUserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserModel> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        ImageView ivProfile;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            ivProfile = itemView.findViewById(R.id.ivProfile);
        }

        void bind(UserModel user) {
            tvName.setText(user.name != null ? user.name : "Unknown");
            tvEmail.setText(user.email != null ? user.email : "");
            tvRole.setText(user.role != null ? user.role.toUpperCase() : "USER");
            
            if (user.profilePicture != null && !user.profilePicture.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.profilePicture)
                        .placeholder(R.drawable.ic_person)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.ic_person);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
