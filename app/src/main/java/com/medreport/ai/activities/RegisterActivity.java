package com.medreport.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.medreport.ai.databinding.ActivityRegisterBinding;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.utils.AuthManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();

        b.btnRegister.setOnClickListener(v -> register());
        b.tvLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String name  = b.etName.getText().toString().trim();
        String email = b.etEmail.getText().toString().trim();
        String pass  = b.etPassword.getText().toString().trim();
        String role  = b.rgRole.getCheckedRadioButtonId() == b.rbDoctor.getId() ? "DOCTOR" : "PATIENT";

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); return;
        }
        setLoading(true);
        auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener(result -> {
            result.getUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
                AuthManager.getInstance().setCachedToken(tokenResult.getToken());
                Map<String, String> body = new HashMap<>();
                body.put("name", name); body.put("email", email); body.put("role", role);
                ApiClient.get().register(body).enqueue(new Callback<ApiResponse<UserModel>>() {
                    @Override public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                        setLoading(false);
                        if (r.isSuccessful() && r.body() != null) AuthManager.getInstance().setCurrentUser(r.body().user);
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                    @Override public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) { setLoading(false); Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show(); }
                });
            });
        }).addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); });
    }

    private void setLoading(boolean l) { b.btnRegister.setEnabled(!l); b.progressBar.setVisibility(l ? View.VISIBLE : View.GONE); }
}
