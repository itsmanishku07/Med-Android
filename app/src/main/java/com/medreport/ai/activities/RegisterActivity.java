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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

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
        b.tvTerms.setOnClickListener(v -> showTermsDialog());
        b.tvLogin.setOnClickListener(v -> finish());
    }


    private void signUpWithGoogle() {
        // Step 1: Sequential Flow for Google starts with Terms
        showProfessionalTermsDialog(true);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            android.util.Log.d("RegisterActivity", "Google Sign-In successful: " + account.getEmail());
            
            // Check if we need role (for new users)
            // But since we want to be safe, we always ask for role if we haven't got it 
            // In a real app, we'd check if user exists.
            // Here, we'll let the backend handle the 'existing user' case, 
            // but we'll show the Role dialog if it's a signup context.
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            setLoading(false);
            android.util.Log.e("RegisterActivity", "Google sign-up failed: " + e.getStatusCode());
            Toast.makeText(this, "Google sign-up failed", Toast.LENGTH_SHORT).show();
        }
    }

    private String pendingName, pendingEmail, pendingPass, pendingRole;

    private void register() {
        pendingName  = b.etName.getText().toString().trim();
        pendingEmail = b.etEmail.getText().toString().trim();
        pendingPass  = b.etPassword.getText().toString().trim();

        if (pendingName.isEmpty() || pendingEmail.isEmpty() || pendingPass.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); 
            return;
        }

        if (pendingPass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start sequential flow: Step 1 - Terms
        showProfessionalTermsDialog(false);
    }

    private void showProfessionalTermsDialog(boolean isGoogle) {
        String termsContent = "MEDICAL DISCLAIMER: MedReport AI provides AI-assisted analysis for informational purposes only. It is NOT a substitute for professional medical advice, diagnosis, or treatment. Always seek the advice of your physician.\n\n" +
                             "TERMS OF SERVICE:\n" +
                             "1. Personal Use: This platform is for personal health tracking.\n" +
                             "2. Privacy: We use industry-standard encryption for your data.\n" +
                             "3. Accuracy: AI results may vary; professional verification is advised.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Legal Compliance")
            .setMessage(termsContent)
            .setCancelable(false)
            .setPositiveButton("I Accept & Continue", (dialog, which) -> {
                if (isGoogle) {
                    // For Google, we proceed to Sign In first, then role
                    setLoading(true);
                    googleSignInClient.signOut().addOnCompleteListener(RegisterActivity.this, task -> {
                        Intent signInIntent = googleSignInClient.getSignInIntent();
                        googleSignInLauncher.launch(signInIntent);
                    });
                } else {
                    showRoleSelectionDialog();
                }
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void showRoleSelectionDialog() {
        String[] roles = {"Patient - I want analysis", "Doctor - I want to review reports"};
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Select Your Account Type")
            .setItems(roles, (dialog, which) -> {
                pendingRole = (which == 0) ? "PATIENT" : "DOCTOR";
                executeSignupAPI();
            })
            .setCancelable(false)
            .show();
    }

    private void executeSignupAPI() {
        setLoading(true);
        
        Map<String, String> body = new HashMap<>();
        body.put("name", pendingName);
        body.put("email", pendingEmail);
        body.put("password", pendingPass);
        body.put("role", pendingRole);

        ApiClient.get().signupRequest(body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override 
            public void onResponse(Call<ApiResponse<Map<String, Object>>> c, Response<ApiResponse<Map<String, Object>>> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null && r.body().success) {
                    userEmail = pendingEmail;
                    emailSent = true;
                    showVerificationScreen();
                } else {
                    String errorMsg = r.body() != null && r.body().message != null ? r.body().message : "Registration failed";
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override 
            public void onFailure(Call<ApiResponse<Map<String, Object>>> c, Throwable t) { 
                setLoading(false); 
                Toast.makeText(RegisterActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    // For Google users, we might still need to ask for role if they are new
                    // Check if additional info says isNewUser
                    boolean isNewUser = authResult.getAdditionalUserInfo().isNewUser();
                    if (isNewUser) {
                        showGoogleRoleSelectionDialog();
                    } else {
                        authResult.getUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
                            AuthManager.getInstance().setCachedToken(tokenResult.getToken());
                            registerWithBackend("PATIENT"); // Default or we can verify
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void showGoogleRoleSelectionDialog() {
        String[] roles = {"Patient", "Doctor"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("One Last Step: Choose Role")
            .setItems(roles, (dialog, which) -> {
                String role = (which == 0) ? "PATIENT" : "DOCTOR";
                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
                    AuthManager.getInstance().setCachedToken(tokenResult.getToken());
                    registerWithBackend(role);
                });
            })
            .setCancelable(false)
            .show();
    }

    private void registerWithBackend(String role) {
        Map<String, String> body = new HashMap<>();
        body.put("role", role);
        
        ApiClient.get().autoRegister(body).enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null && r.body().user != null) {
                    AuthManager.getInstance().setCurrentUser(r.body().user);
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVerificationScreen() {
        b.layoutRegisterForm.setVisibility(View.GONE);
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

    private void showTermsDialog() {
        showProfessionalTermsDialog(false);
    }
}
