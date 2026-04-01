package com.medreport.ai.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.medreport.ai.R;
import com.medreport.ai.models.UserModel;
import com.medreport.ai.utils.AuthManager;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }

        UserModel user = AuthManager.getInstance().getCurrentUser();
        if (user != null) {
            TextView tvName = findViewById(R.id.tvName);
            TextView tvEmail = findViewById(R.id.tvEmail);
            TextView tvRole = findViewById(R.id.tvRole);
            if (tvName != null) tvName.setText(user.name != null ? user.name : "Unknown User");
            if (tvEmail != null) tvEmail.setText(user.email != null ? user.email : "No Email Provided");
            if (tvRole != null) tvRole.setText(user.role != null ? user.role : "PATIENT");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
