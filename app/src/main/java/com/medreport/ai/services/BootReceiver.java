package com.medreport.ai.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.medreport.ai.network.ApiClient;
import com.medreport.ai.models.ResponseModels;
import com.medreport.ai.models.WaterReminderSettings;
import com.medreport.ai.utils.AuthManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        
        AuthManager.getInstance().refreshToken(new AuthManager.TokenCallback() {
            @Override public void onToken(String token) {
                ApiClient.get().getReminders().enqueue(new Callback<ResponseModels.RemindersResponse>() {
                    @Override public void onResponse(Call<ResponseModels.RemindersResponse> call, Response<ResponseModels.RemindersResponse> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().reminders != null) {
                            ReminderScheduler.scheduleAll(ctx, r.body().reminders);
                        }
                    }
                    @Override public void onFailure(Call<ResponseModels.RemindersResponse> call, Throwable t) {}
                });
            }
            @Override public void onError(Exception e) {}
        });
        
        WaterReminderSettings waterSettings = WaterReminderSettings.load(ctx);
        if (waterSettings.isEnabled()) {
            WaterReminderScheduler.schedule(ctx, waterSettings);
        }
    }
}
