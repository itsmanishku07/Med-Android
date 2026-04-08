package com.medreport.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.medreport.ai.R;
import com.medreport.ai.databinding.ActivityMainBinding;
import com.medreport.ai.fragments.*;
import com.medreport.ai.utils.AuthManager;
import com.medreport.ai.models.UserModel;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;
    private int currentTabId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            b = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(b.getRoot());
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Layout Init Err: " + e.getMessage(), android.widget.Toast.LENGTH_LONG)
                    .show();
            return;
        }

        try {
            UserModel user = AuthManager.getInstance().getCurrentUser();
            setupNav(user);

            if (savedInstanceState == null) {
                String target = getIntent().getStringExtra("target_tab");
                if ("reminders".equals(target)) {
                    b.bottomNav.setSelectedItemId(R.id.nav_reminders);
                    loadFragment(new RemindersFragment());
                } else if (user != null && user.isAdmin()) {
                    // Admin users start with Admin Dashboard
                    b.bottomNav.setSelectedItemId(R.id.nav_admin);
                    loadFragment(new AdminDashboardFragment());
                } else {
                    loadFragment(new DashboardFragment());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Logic Init Err: " + e.getMessage(), android.widget.Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void setupNav(UserModel user) {
        try {
            b.btnNotifs.setOnClickListener(v -> loadFragment(new NotificationsFragment()));

            if (user != null && user.isAdmin()) {
                // For admin users, show only Admin and Profile tabs
                b.bottomNav.getMenu().clear();
                b.bottomNav.getMenu().add(0, R.id.nav_admin, 0, "Admin")
                        .setIcon(R.drawable.ic_admin);
                b.bottomNav.getMenu().add(0, R.id.nav_profile, 1, "Profile")
                        .setIcon(R.drawable.ic_user);
            } else if (user != null && user.isDoctor()) {
                MenuItem remindersItem = b.bottomNav.getMenu().findItem(R.id.nav_reminders);
                if (remindersItem != null)
                    remindersItem.setVisible(false);
            }

            b.bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard)
                    loadFragment(new DashboardFragment());
                else if (id == R.id.nav_reports)
                    loadFragment(new ReportsFragment());
                else if (id == R.id.nav_chats)
                    loadFragment(new ChatsFragment());
                else if (id == R.id.nav_reminders)
                    loadFragment(new RemindersFragment());
                else if (id == R.id.nav_profile)
                    loadFragment(new ProfileFragment());
                else if (id == R.id.nav_admin)
                    loadFragment(new AdminDashboardFragment());
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFragment(Fragment f) {
        int newTabId = b.bottomNav.getSelectedItemId();
        int oldIndex = getTabIndex(currentTabId);
        int newIndex = getTabIndex(newTabId);

        try {
            androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            
            if (currentTabId != -1) {
                if (newIndex > oldIndex) {
                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (newIndex < oldIndex) {
                    ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
            
            ft.replace(R.id.fragmentContainer, f);
            ft.commit();
            currentTabId = newTabId;
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Nav Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG)
                    .show();
        }
    }

    private int getTabIndex(int itemId) {
        if (itemId == R.id.nav_dashboard) return 0;
        if (itemId == R.id.nav_reports) return 1;
        if (itemId == R.id.nav_chats) return 2;
        if (itemId == R.id.nav_reminders) return 3;
        if (itemId == R.id.nav_admin) return 0; // Admin tab at position 0
        if (itemId == R.id.nav_profile) return 1; // Profile tab at position 1
        return -1;
    }
}
