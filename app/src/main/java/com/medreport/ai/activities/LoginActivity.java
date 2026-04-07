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
import com.medreport.ai.databinding.ActivityLoginBinding;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.utils.AuthManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
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
                        Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        b.btnLogin.setOnClickListener(v -> login());
        b.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        b.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void signInWithGoogle() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            android.util.Log.d("LoginActivity", "Google Sign-In successful: " + account.getEmail());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            setLoading(false);
            android.util.Log.e("LoginActivity", "Google sign-in failed with code: " + e.getStatusCode(), e);
            String errorMessage = "Google sign-in failed";
            
            // Provide more specific error messages
            switch (e.getStatusCode()) {
                case 10:
                    errorMessage = "Google Sign-In configuration error. Please check your setup.";
                    break;
                case 12500:
                    errorMessage = "Google Sign-In is not configured properly.";
                    break;
                case 12501:
                    errorMessage = "Sign-in cancelled";
                    break;
                default:
                    errorMessage = "Google sign-in failed: " + e.getStatusCode();
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
                        autoRegisterAndFetchProfile();
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void autoRegisterAndFetchProfile() {
        // Try to fetch profile first, if fails then auto-register
        ApiClient.get().getProfile().enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().user != null) {
                    AuthManager.getInstance().setCurrentUser(r.body().user);
                    navigateToMain();
                } else {
                    // User doesn't exist, auto-register with default role
                    autoRegister();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) {
                // On failure, try auto-register
                autoRegister();
            }
        });
    }

    private void autoRegister() {
        Map<String, String> body = new HashMap<>();
        // Default role for Google sign-in is PATIENT
        ApiClient.get().autoRegister(body).enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().user != null) {
                    AuthManager.getInstance().setCurrentUser(r.body().user);
                }
                navigateToMain();
            }

            @Override
            public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) {
                navigateToMain();
            }
        });
    }

    private void login() {
        String email = b.etEmail.getText().toString().trim();
        String pass  = b.etPassword.getText().toString().trim();
        if (email.isEmpty() || pass.isEmpty()) { 
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); 
            return; 
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener(result -> {
            result.getUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
                AuthManager.getInstance().setCachedToken(tokenResult.getToken());
                fetchProfile();
            });
        }).addOnFailureListener(e -> { 
            setLoading(false); 
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); 
        });
    }

    private void fetchProfile() {
        ApiClient.get().getProfile().enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override 
            public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null) {
                    AuthManager.getInstance().setCurrentUser(r.body().user);
                }
                navigateToMain();
            }
            
            @Override 
            public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) {
                setLoading(false);
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        setLoading(false);
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        b.btnLogin.setEnabled(!loading);
        b.btnGoogleSignIn.setEnabled(!loading);
        b.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
