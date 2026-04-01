package com.medreport.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.medreport.ai.databinding.ActivityLoginBinding;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.utils.AuthManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();

        b.btnLogin.setOnClickListener(v -> login());
        b.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = b.etEmail.getText().toString().trim();
        String pass  = b.etPassword.getText().toString().trim();
        if (email.isEmpty() || pass.isEmpty()) { Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); return; }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener(result -> {
            result.getUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
                AuthManager.getInstance().setCachedToken(tokenResult.getToken());
                fetchProfile();
            });
        }).addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); });
    }

    private void fetchProfile() {
        ApiClient.get().getProfile().enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null) {
                    AuthManager.getInstance().setCurrentUser(r.body().user);
                }
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
            @Override public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) {
                setLoading(false);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        b.btnLogin.setEnabled(!loading);
        b.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
