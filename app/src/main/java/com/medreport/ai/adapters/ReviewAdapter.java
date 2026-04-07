package com.medreport.ai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.databinding.ItemReviewBinding;
import com.medreport.ai.models.DoctorReview;
import com.medreport.ai.utils.AuthManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    
    public interface OnReviewActionListener {
        void onAction(DoctorReview review, String action);
    }
    
    private final List<DoctorReview> reviews;
    private final OnReviewActionListener listener;
    private final String currentUserId;
    
    public ReviewAdapter(List<DoctorReview> reviews, OnReviewActionListener listener) {
        this.reviews = reviews;
        this.listener = listener;
        var user = AuthManager.getInstance().getCurrentUser();
        this.currentUserId = user != null ? user.id : null;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }
    
    @Override
    public int getItemCount() {
        return reviews.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReviewBinding b;
        
        ViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
        
        void bind(DoctorReview review) {
            b.tvPatientName.setText(review.patient.name);
            b.ratingBar.setRating(review.rating);
            b.tvRating.setText(String.valueOf(review.rating));
            
            if (review.comment != null && !review.comment.isEmpty()) {
                b.tvComment.setText(review.comment);
                b.tvComment.setVisibility(View.VISIBLE);
            } else {
                b.tvComment.setVisibility(View.GONE);
            }
            
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdf.parse(review.created_at);
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                b.tvDate.setText(displayFormat.format(date));
            } catch (Exception e) {
                b.tvDate.setText(review.created_at);
            }
            
            if (currentUserId != null && currentUserId.equals(review.patient.id)) {
                b.btnDelete.setVisibility(View.VISIBLE);
                b.btnDelete.setOnClickListener(v -> listener.onAction(review, "delete"));
            } else {
                b.btnDelete.setVisibility(View.GONE);
            }
        }
    }
}
