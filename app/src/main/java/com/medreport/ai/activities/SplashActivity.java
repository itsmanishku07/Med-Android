package com.medreport.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.utils.AuthManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            go(LoginActivity.class);
            return;
        }
        user.getIdToken(true).addOnSuccessListener(result -> {
            AuthManager.getInstance().setCachedToken(result.getToken());
            ApiClient.get().getProfile().enqueue(new Callback<ApiResponse<UserModel>>() {
                @Override public void onResponse(Call<ApiResponse<UserModel>> c, Response<ApiResponse<UserModel>> r) {
                    if (r.isSuccessful() && r.body() != null && r.body().success) {
                        AuthManager.getInstance().setCurrentUser(r.body().user);
                    }
                    go(MainActivity.class);
                }
                @Override public void onFailure(Call<ApiResponse<UserModel>> c, Throwable t) { go(MainActivity.class); }
            });
        }).addOnFailureListener(e -> go(LoginActivity.class));
    }

    private void go(Class<?> cls) {
        startActivity(new Intent(this, cls));
        finish();
    }
}
