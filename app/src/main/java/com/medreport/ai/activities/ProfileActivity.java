package com.medreport.ai.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.medreport.ai.databinding.ActivityProfileBinding;
import com.medreport.ai.models.ApiResponse;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.utils.AuthManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding b;
    private String base64Image = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        setupImagePicker();
        loadUserData();

        b.btnSave.setOnClickListener(v -> saveProfile());
        b.fabEditPic.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    Bitmap bitmap;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    }
                    
                    b.ivProfilePic.setImageBitmap(bitmap);
                    b.tvInitial.setVisibility(View.GONE);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
                    byte[] byteArray = outputStream.toByteArray();
                    base64Image = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadUserData() {
        UserModel user = AuthManager.getInstance().getCurrentUser();
        if (user == null) return;

        b.tvHeaderName.setText(user.name);
        b.tvHeaderRole.setText(user.role);
        b.etName.setText(user.name);
        b.etPhone.setText(user.phone);

        if (user.profile != null) {
            b.etLocation.setText(user.profile.location);
            b.etBio.setText(user.profile.bio);
        }

        if (user.isDoctor()) {
            b.cardSpecializations.setVisibility(View.VISIBLE);
            if (user.specializations != null) {
                b.etSpecializations.setText(String.join(", ", user.specializations));
            }
        }

        if (user.profilePicture != null && user.profilePicture.startsWith("data:image")) {
            try {
                String pureBase64 = user.profilePicture.split(",")[1];
                byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                b.ivProfilePic.setImageBitmap(decodedByte);
                b.tvInitial.setVisibility(View.GONE);
            } catch (Exception e) {
                showInitial(user.name);
            }
        } else {
            showInitial(user.name);
        }
    }

    private void showInitial(String name) {
        b.tvInitial.setVisibility(View.VISIBLE);
        b.tvInitial.setText(name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)) : "U");
    }

    private void saveProfile() {
        String name = b.etName.getText().toString().trim();
        String phone = b.etPhone.getText().toString().trim();
        String location = b.etLocation.getText().toString().trim();
        String bio = b.etBio.getText().toString().trim();
        String specsStr = b.etSpecializations.getText().toString().trim();

        if (name.isEmpty()) {
            b.etName.setError("Name is required");
            return;
        }

        b.progressBar.setVisibility(View.VISIBLE);
        b.btnSave.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("phone", phone);
        
        Map<String, String> profile = new HashMap<>();
        profile.put("location", location);
        profile.put("bio", bio);
        body.put("profile", profile);

        if (base64Image != null) {
            body.put("profile_picture", base64Image);
        }

        UserModel user = AuthManager.getInstance().getCurrentUser();
        if (user != null && user.isDoctor()) {
            List<String> specs = new ArrayList<>();
            if (!specsStr.isEmpty()) {
                specs = Arrays.asList(specsStr.split("\\s*,\\s*"));
            }
            body.put("specializations", specs);
        }

        ApiClient.get().updateProfile(body).enqueue(new Callback<ApiResponse<UserModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserModel>> call, Response<ApiResponse<UserModel>> response) {
                b.progressBar.setVisibility(View.GONE);
                b.btnSave.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    AuthManager.getInstance().setCurrentUser(response.body().user);
                    Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserModel>> call, Throwable t) {
                b.progressBar.setVisibility(View.GONE);
                b.btnSave.setEnabled(true);
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
