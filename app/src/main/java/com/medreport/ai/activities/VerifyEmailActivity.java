package com.medreport.ai.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.medreport.ai.databinding.ActivityVerifyEmailBinding;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.utils.AuthManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;

public class VerifyEmailActivity extends AppCompatActivity {
    private ActivityVerifyEmailBinding b;
    private String verificationToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null) {
            verificationToken = data.getQueryParameter("token");
            if (verificationToken != null && !verificationToken.isEmpty()) {
                verifyEmail();
            } else {
                showError("Invalid verification link");
            }
        } else {
            showError("No verification token found");
        }

        b.btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        b.btnGoToLoginError.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void verifyEmail() {
        b.progressBar.setVisibility(View.VISIBLE);
        b.layoutSuccess.setVisibility(View.GONE);
        b.layoutError.setVisibility(View.GONE);

        Map<String, String> body = new HashMap<>();
        body.put("token", verificationToken);

        ApiClient.get().signupVerify(body).enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                b.progressBar.setVisibility(View.GONE);
                
                if (r.isSuccessful() && r.body() != null && r.body().success) {
                    showSuccess();
                } else {
                    String errorMsg = r.body() != null && r.body().message != null 
                        ? r.body().message 
                        : "Verification failed";
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void showSuccess() {
        b.layoutSuccess.setVisibility(View.VISIBLE);
        b.layoutError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        b.layoutError.setVisibility(View.VISIBLE);
        b.layoutSuccess.setVisibility(View.GONE);
        b.tvErrorMessage.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
