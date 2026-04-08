package com.medreport.ai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.medreport.ai.R;
import com.medreport.ai.adapters.ReviewAdapter;
import com.medreport.ai.databinding.ActivityDoctorProfileBinding;
import com.medreport.ai.models.*;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.utils.AuthManager;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorProfileActivity extends AppCompatActivity {
    public static final String EXTRA_DOCTOR_ID = "doctor_id";
    
    private ActivityDoctorProfileBinding b;
    private String doctorId;
    private UserModel doctor;
    private List<DoctorReview> reviews = new ArrayList<>();
    private ReviewStats stats;
    private DoctorReview myReview;
    private ReviewAdapter reviewAdapter;
    private int selectedRating = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityDoctorProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        
        doctorId = getIntent().getStringExtra(EXTRA_DOCTOR_ID);
        if (doctorId == null) {
            finish();
            return;
        }
        
        setupToolbar();
        setupRecyclerView();
        loadDoctorProfile();
        loadReviews();
        loadMyReview();
        setupListeners();
    }
    
    private void setupToolbar() {
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Doctor Profile");
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(reviews, this::onReviewAction);
        b.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        b.rvReviews.setAdapter(reviewAdapter);
    }
    
    private void setupListeners() {
        UserModel currentUser = AuthManager.getInstance().getCurrentUser();
        boolean isPatient = currentUser != null && "PATIENT".equals(currentUser.role);
        
        if (isPatient) {
            b.btnWriteReview.setVisibility(View.VISIBLE);
            b.btnWriteReview.setOnClickListener(v -> showReviewDialog());
        } else {
            b.btnWriteReview.setVisibility(View.GONE);
        }
    }
    
    private void loadDoctorProfile() {
        b.progressBar.setVisibility(View.VISIBLE);
        ApiClient.get().getDoctors().enqueue(new Callback<ResponseModels.DoctorsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.DoctorsResponse> call, Response<ResponseModels.DoctorsResponse> response) {
                b.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    for (UserModel doc : response.body().doctors) {
                        if (doc.id.equals(doctorId)) {
                            doctor = doc;
                            displayDoctorInfo();
                            break;
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ResponseModels.DoctorsResponse> call, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                Toast.makeText(DoctorProfileActivity.this, "Failed to load doctor", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void displayDoctorInfo() {
        if (doctor == null) return;
        
        b.tvDoctorName.setText("Dr. " + doctor.name);
        
        if (doctor.specializations != null && !doctor.specializations.isEmpty()) {
            b.tvSpecialization.setText(doctor.specializations.get(0));
        }
        
        b.tvEmail.setText(doctor.email);
        
        if (doctor.profile != null) {
            if (doctor.profile.experience != null) {
                b.tvExperience.setText(doctor.profile.experience + " Years");
                b.tvExperience.setVisibility(View.VISIBLE);
            }
            if (doctor.profile.location != null) {
                b.tvLocation.setText(doctor.profile.location);
                b.tvLocation.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void loadReviews() {
        ApiClient.get().getDoctorReviews(doctorId).enqueue(new Callback<ResponseModels.DoctorReviewsResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.DoctorReviewsResponse> call, Response<ResponseModels.DoctorReviewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reviews.clear();
                    if (response.body().reviews != null) {
                        reviews.addAll(response.body().reviews);
                    }
                    stats = response.body().stats;
                    
                    reviewAdapter.notifyDataSetChanged();
                    displayRatingStats();
                    
                    b.tvNoReviews.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            
            @Override
            public void onFailure(Call<ResponseModels.DoctorReviewsResponse> call, Throwable t) {
                Toast.makeText(DoctorProfileActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void displayRatingStats() {
        if (stats == null) return;
        
        b.tvAverageRating.setText(String.format(Locale.US, "%.1f", stats.average_rating));
        b.tvTotalReviews.setText(stats.total_reviews + " reviews");
        b.ratingBar.setRating((float) stats.average_rating);
    }
    
    private void loadMyReview() {
        UserModel currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null || !"PATIENT".equals(currentUser.role)) return;
        
        ApiClient.get().getMyReview(doctorId).enqueue(new Callback<ResponseModels.MyReviewResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.MyReviewResponse> call, Response<ResponseModels.MyReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myReview = response.body().review;
                    if (myReview != null) {
                        b.btnWriteReview.setText("Edit Review");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ResponseModels.MyReviewResponse> call, Throwable t) {
            }
        });
    }
    
    private void showReviewDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        
        MaterialButton[] starButtons = new MaterialButton[5];
        starButtons[0] = dialogView.findViewById(R.id.btnStar1);
        starButtons[1] = dialogView.findViewById(R.id.btnStar2);
        starButtons[2] = dialogView.findViewById(R.id.btnStar3);
        starButtons[3] = dialogView.findViewById(R.id.btnStar4);
        starButtons[4] = dialogView.findViewById(R.id.btnStar5);
        
        TextInputEditText etComment = dialogView.findViewById(R.id.etComment);
        
        if (myReview != null) {
            selectedRating = myReview.rating;
            updateStarButtons(starButtons, selectedRating);
            if (myReview.comment != null) {
                etComment.setText(myReview.comment);
            }
        } else {
            selectedRating = 0;
        }
        
        for (int i = 0; i < starButtons.length; i++) {
            final int rating = i + 1;
            starButtons[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateStarButtons(starButtons, rating);
            });
        }
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(myReview != null ? "Edit Review" : "Write Review")
                .setView(dialogView)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (selectedRating == 0) {
                    Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String comment = etComment.getText().toString().trim();
                submitReview(selectedRating, comment);
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    private void updateStarButtons(MaterialButton[] buttons, int rating) {
        for (int i = 0; i < buttons.length; i++) {
            if (i < rating) {
                buttons[i].setIconResource(android.R.drawable.btn_star_big_on);
            } else {
                buttons[i].setIconResource(android.R.drawable.btn_star_big_off);
            }
        }
    }
    
    private void submitReview(int rating, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("rating", rating);
        body.put("comment", comment);
        
        ApiClient.get().submitReview(doctorId, body).enqueue(new Callback<ResponseModels.SubmitReviewResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.SubmitReviewResponse> call, Response<ResponseModels.SubmitReviewResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DoctorProfileActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                    loadReviews();
                    loadMyReview();
                } else {
                    Toast.makeText(DoctorProfileActivity.this, "Failed to submit review", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ResponseModels.SubmitReviewResponse> call, Throwable t) {
                Toast.makeText(DoctorProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void onReviewAction(DoctorReview review, String action) {
        if ("delete".equals(action)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Review")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteReview(review.id))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
    
    private void deleteReview(String reviewId) {
        ApiClient.get().deleteReview(reviewId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DoctorProfileActivity.this, "Review deleted", Toast.LENGTH_SHORT).show();
                    myReview = null;
                    b.btnWriteReview.setText("Write Review");
                    loadReviews();
                } else {
                    Toast.makeText(DoctorProfileActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DoctorProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
