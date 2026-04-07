package com.medreport.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.medreport.ai.R;
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
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean emailSent = false;
    private String userEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Register activity result launcher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Google sign-up cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        b.btnRegister.setOnClickListener(v -> register());
        b.btnGoogleSignUp.setOnClickListener(v -> signUpWithGoogle());
        b.tvLogin.setOnClickListener(v -> finish());
    }

    private void signUpWithGoogle() {
        // Check if role is selected
        int selectedRoleId = b.rgRole.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select account type first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            android.util.Log.d("RegisterActivity", "Google Sign-In successful: " + account.getEmail());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            setLoading(false);
            android.util.Log.e("RegisterActivity", "Google sign-up failed with code: " + e.getStatusCode(), e);
            String errorMessage = "Google sign-up failed";
            
            // Provide more specific error messages
            switch (e.getStatusCode()) {
                case 10:
                    errorMessage = "Google Sign-In configuration error. Please check your setup.";
                    break;
                case 12500:
                    errorMessage = "Google Sign-In is not configured properly.";
                    break;
                case 12501:
                    errorMessage = "Sign-up cancelled";
                    break;
                default:
                    errorMessage = "Google sign-up failed: " + e.getStatusCode();
            }
            
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    authResult.getUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
                        AuthManager.getInstance().setCachedToken(tokenResult.getToken());
                        registerWithBackend();
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void registerWithBackend() {
        String role = b.rgRole.getCheckedRadioButtonId() == b.rbDoctor.getId() ? "DOCTOR" : "PATIENT";
        
        Map<String, String> body = new HashMap<>();
        body.put("role", role);
        
        ApiClient.get().autoRegister(body).enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null && r.body().user != null) {
                    AuthManager.getInstance().setCurrentUser(r.body().user);
                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    String errorMsg = r.body() != null && r.body().message != null 
                        ? r.body().message 
                        : "Registration failed";
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void register() {
        String name  = b.etName.getText().toString().trim();
        String email = b.etEmail.getText().toString().trim();
        String pass  = b.etPassword.getText().toString().trim();
        String role  = b.rgRole.getCheckedRadioButtonId() == b.rbDoctor.getId() ? "DOCTOR" : "PATIENT";

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); 
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", pass);
        body.put("role", role);

        ApiClient.get().signupRequest(body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override 
            public void onResponse(Call<ApiResponse<Map<String, Object>>> c, Response<ApiResponse<Map<String, Object>>> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null && r.body().success) {
                    userEmail = email;
                    emailSent = true;
                    showVerificationScreen();
                    Toast.makeText(RegisterActivity.this, "Verification email sent!", Toast.LENGTH_LONG).show();
                } else {
                    String errorMsg = r.body() != null && r.body().message != null 
                        ? r.body().message 
                        : "Registration failed";
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override 
            public void onFailure(Call<ApiResponse<Map<String, Object>>> c, Throwable t) { 
                setLoading(false); 
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showVerificationScreen() {
        // Hide registration form
        b.layoutRegisterForm.setVisibility(View.GONE);
        
        // Show verification message
        b.layoutVerificationSent.setVisibility(View.VISIBLE);
        b.tvVerificationEmail.setText(userEmail);
        
        b.btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setLoading(boolean l) { 
        b.btnRegister.setEnabled(!l);
        b.btnGoogleSignUp.setEnabled(!l);
        b.progressBar.setVisibility(l ? View.VISIBLE : View.GONE); 
    }
}
