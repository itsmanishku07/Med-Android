package com.medreport.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medreport.ai.R;
import com.medreport.ai.adapters.TableDataAdapter;
import com.medreport.ai.databinding.ActivityAdminDashboardBinding;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding b;
    private TableDataAdapter dataAdapter;
    private String selectedTableName;
    private int currentPage = 1;
    private int totalPages = 1;
    private List<ResponseModels.DatabaseTable> allTables = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setupToolbar();
        setupRecyclerViews();
        setupListeners();
        loadTables();
    }

    private void setupToolbar() {
        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        b.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        // Table data
        dataAdapter = new TableDataAdapter(row -> showDeleteConfirmation(row));
        b.rvTableData.setLayoutManager(new LinearLayoutManager(this));
        b.rvTableData.setAdapter(dataAdapter);
    }

    private void setupListeners() {
        b.btnRefreshTables.setOnClickListener(v -> loadTables());
        b.btnClearTable.setOnClickListener(v -> showClearTableConfirmation());
        b.btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                loadTableData(selectedTableName, currentPage - 1);
            }
        });
        b.btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                loadTableData(selectedTableName, currentPage + 1);
            }
        });
        
        // Dropdown selection listener
        b.tableSpinner.setOnItemClickListener((parent, view, position, id) -> {
            ResponseModels.DatabaseTable selectedTable = allTables.get(position);
            selectedTableName = selectedTable.name;
            currentPage = 1;
            loadTableData(selectedTable.name, 1);
        });
    }

    private void loadTables() {
        b.progressTables.setVisibility(View.VISIBLE);
        b.tableSpinnerLayout.setVisibility(View.GONE);

        ApiClient.get().getDatabaseTables().enqueue(new Callback<ResponseModels.DatabaseTablesResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.DatabaseTablesResponse> call, Response<ResponseModels.DatabaseTablesResponse> response) {
                b.progressTables.setVisibility(View.GONE);
                b.tableSpinnerLayout.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    allTables = response.body().tables;
                    setupTableDropdown();
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to load tables", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.DatabaseTablesResponse> call, Throwable t) {
                b.progressTables.setVisibility(View.GONE);
                b.tableSpinnerLayout.setVisibility(View.VISIBLE);
                Toast.makeText(AdminDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupTableDropdown() {
        List<String> tableNames = new ArrayList<>();
        for (ResponseModels.DatabaseTable table : allTables) {
            tableNames.add(table.name + " (" + table.rowCount + " rows)");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            tableNames
        );
        b.tableSpinner.setAdapter(adapter);
    }

    private void loadTableData(String tableName, int page) {
        b.progressTableData.setVisibility(View.VISIBLE);
        b.layoutEmptyState.setVisibility(View.GONE);
        b.layoutTableContent.setVisibility(View.GONE);

        ApiClient.get().getTableData(tableName, page, 50).enqueue(new Callback<ResponseModels.TableDataResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.TableDataResponse> call, Response<ResponseModels.TableDataResponse> response) {
                b.progressTableData.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    ResponseModels.TableData data = response.body().data;
                    
                    b.layoutTableContent.setVisibility(View.VISIBLE);
                    b.tvSelectedTable.setText(tableName);
                    b.tvTotalRecords.setText(data.total + " total records");
                    
                    // Show warning for users table
                    b.cardUserWarning.setVisibility(tableName.equals("users") ? View.VISIBLE : View.GONE);
                    
                    // Update data
                    dataAdapter.setData(data.columns, data.rows);
                    
                    // Update pagination
                    currentPage = data.page;
                    totalPages = data.totalPages;
                    
                    if (totalPages > 1) {
                        b.layoutPagination.setVisibility(View.VISIBLE);
                        b.tvPageInfo.setText("Page " + currentPage + " of " + totalPages);
                        b.btnPrevPage.setEnabled(currentPage > 1);
                        b.btnNextPage.setEnabled(currentPage < totalPages);
                    } else {
                        b.layoutPagination.setVisibility(View.GONE);
                    }
                } else {
                    b.layoutEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(AdminDashboardActivity.this, "Failed to load table data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.TableDataResponse> call, Throwable t) {
                b.progressTableData.setVisibility(View.GONE);
                b.layoutEmptyState.setVisibility(View.VISIBLE);
                Toast.makeText(AdminDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(Map<String, Object> row) {
        Object idObj = row.get("id");
        if (idObj == null) {
            idObj = row.values().iterator().next();
        }
        
        String recordId = idObj.toString();
        boolean isUsersTable = selectedTableName.equals("users");
        
        new AlertDialog.Builder(this)
            .setTitle(isUsersTable ? "Delete User?" : "Delete Record?")
            .setMessage(isUsersTable 
                ? "This will:\n• Remove the user from the database\n• Delete their Firebase Authentication account\n• This action cannot be undone!"
                : "Are you sure you want to delete this record? This action cannot be undone!")
            .setPositiveButton("Yes, Delete", (dialog, which) -> deleteRecord(recordId))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteRecord(String recordId) {
        if (selectedTableName.equals("users")) {
            // Delete user completely
            ApiClient.get().deleteUserCompletely(recordId).enqueue(new Callback<ResponseModels.DeleteUserResponse>() {
                @Override
                public void onResponse(Call<ResponseModels.DeleteUserResponse> call, Response<ResponseModels.DeleteUserResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        String msg = response.body().deletedFromFirebase
                            ? "User deleted from database and Firebase"
                            : "User deleted from database (Firebase deletion failed)";
                        Toast.makeText(AdminDashboardActivity.this, msg, Toast.LENGTH_SHORT).show();
                        loadTableData(selectedTableName, currentPage);
                        loadTables();
                    } else {
                        Toast.makeText(AdminDashboardActivity.this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseModels.DeleteUserResponse> call, Throwable t) {
                    Toast.makeText(AdminDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Delete regular record
            ApiClient.get().deleteRecord(selectedTableName, recordId).enqueue(new Callback<com.medreport.ai.models.ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<com.medreport.ai.models.ApiResponse<Void>> call, Response<com.medreport.ai.models.ApiResponse<Void>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        Toast.makeText(AdminDashboardActivity.this, "Record deleted successfully", Toast.LENGTH_SHORT).show();
                        loadTableData(selectedTableName, currentPage);
                        loadTables();
                    } else {
                        Toast.makeText(AdminDashboardActivity.this, "Failed to delete record", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<com.medreport.ai.models.ApiResponse<Void>> call, Throwable t) {
                    Toast.makeText(AdminDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showClearTableConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Table?")
            .setMessage("Are you sure you want to delete ALL records from " + selectedTableName + "? This action cannot be undone!")
            .setPositiveButton("Yes, Clear Table", (dialog, which) -> clearTable())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void clearTable() {
        ApiClient.get().clearTable(selectedTableName, "yes").enqueue(new Callback<ResponseModels.ClearTableResponse>() {
            @Override
            public void onResponse(Call<ResponseModels.ClearTableResponse> call, Response<ResponseModels.ClearTableResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(AdminDashboardActivity.this, 
                        "Cleared " + response.body().deletedCount + " records from " + selectedTableName, 
                        Toast.LENGTH_SHORT).show();
                    loadTableData(selectedTableName, 1);
                    loadTables();
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to clear table", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseModels.ClearTableResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
