package com.medreport.ai.fragments;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.medreport.ai.R;

import java.util.ArrayList;
import java.util.List;

public class ReportActionsBottomSheet extends BottomSheetDialogFragment {

    public interface OnActionSelectedListener {
        void onActionSelected(String action);
    }

    private OnActionSelectedListener listener;
    private final List<ActionItem> actions = new ArrayList<>();

    public static ReportActionsBottomSheet newInstance() {
        return new ReportActionsBottomSheet();
    }

    public void setListener(OnActionSelectedListener listener) {
        this.listener = listener;
    }

    public void addAction(String id, String label, int iconRes, int colorRes) {
        actions.add(new ActionItem(id, label, iconRes, colorRes));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_report_actions, container, false);
        LinearLayout actionContainer = view.findViewById(R.id.actionContainer);

        for (ActionItem item : actions) {
            View itemView = createActionView(item);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onActionSelected(item.id);
                dismiss();
            });
            actionContainer.addView(itemView);
        }

        return view;
    }

    private View createActionView(ActionItem item) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        layout.setBackgroundResource(R.drawable.bg_badge);
        layout.setBackgroundTintList(ColorStateList.valueOf(0x081A73E8)); // Subtle blue
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(10);
        layout.setLayoutParams(lp);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(item.iconRes);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), item.colorRes)));
        layout.addView(icon);

        TextView text = new TextView(getContext());
        text.setText(item.label);
        text.setTextSize(16);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dpToPx(20);
        text.setLayoutParams(tlp);
        layout.addView(text);

        return layout;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class ActionItem {
        String id;
        String label;
        int iconRes;
        int colorRes;

        ActionItem(String id, String label, int iconRes, int colorRes) {
            this.id = id;
            this.label = label;
            this.iconRes = iconRes;
            this.colorRes = colorRes;
        }
    }
}
