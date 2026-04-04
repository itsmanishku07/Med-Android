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
            // Handle Top Notification Button
            b.btnNotifs.setOnClickListener(v -> loadFragment(new NotificationsFragment()));

            // Menu is already inflated via activity_main.xml (app:menu)
            // We only need to adjust visibility for admin/doctor roles here.
            if (user != null && user.isAdmin()) {
                // To keep within 5-item limit, replace Reminders with Admin
                MenuItem remindersItem = b.bottomNav.getMenu().findItem(R.id.nav_reminders);
                if (remindersItem != null) {
                    remindersItem.setTitle("Admin");
                    remindersItem.setIcon(R.drawable.ic_admin);
                    // We'll reuse the reminders ID for Admin for simplicity, or we can use the
                    // actual ID.
                    // Let's use the actual ID by removing and adding, but careful with the 5-item
                    // limit.
                    b.bottomNav.getMenu().removeItem(R.id.nav_reminders);
                    b.bottomNav.getMenu().add(android.view.Menu.NONE, R.id.nav_admin, 10, "Admin")
                            .setIcon(R.drawable.ic_admin);
                }
            } else if (user != null && user.isDoctor()) {
                // Doctors don't use personal reminders usually
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
        if (itemId == R.id.nav_reminders || itemId == R.id.nav_admin) return 3;
        if (itemId == R.id.nav_profile) return 4;
        return -1;
    }
}
