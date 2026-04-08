package com.medreport.ai.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableDataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ROW = 1;
    private static final int COLUMN_WIDTH_DP = 160;
    private static final int ACTION_COLUMN_WIDTH_DP = 90;

    private List<String> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    private OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Map<String, Object> row);
    }

    public TableDataAdapter(OnDeleteClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<String> columns, List<Map<String, Object>> rows) {
        this.columns = columns != null ? columns : new ArrayList<>();
        this.rows = rows != null ? rows : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_table_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_table_row, parent, false);
            return new RowViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(columns);
        } else if (holder instanceof RowViewHolder) {
            ((RowViewHolder) holder).bind(columns, rows.get(position - 1), position);
        }
    }

    @Override
    public int getItemCount() {
        return rows.isEmpty() ? 0 : rows.size() + 1;
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        LinearLayout headerContainer;

        HeaderViewHolder(View itemView) {
            super(itemView);
            headerContainer = itemView.findViewById(R.id.headerContainer);
        }

        void bind(List<String> columns) {
            headerContainer.removeAllViews();
            Context context = headerContainer.getContext();
            
            for (String column : columns) {
                TextView tv = createHeaderCell(context, column);
                headerContainer.addView(tv);
            }
            
            // Actions column
            TextView actionsCell = createHeaderCell(context, "ACTIONS");
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) actionsCell.getLayoutParams();
            params.width = dpToPx(context, ACTION_COLUMN_WIDTH_DP);
            params.height = dpToPx(context, 48); // Fixed height
            params.setMargins(0, 0, 0, 0); // No margins
            actionsCell.setLayoutParams(params);
            actionsCell.setGravity(android.view.Gravity.CENTER);
            headerContainer.addView(actionsCell);
        }

        private TextView createHeaderCell(Context context, String text) {
            TextView tv = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(context, COLUMN_WIDTH_DP),
                dpToPx(context, 48) // Fixed height
            );
            params.setMargins(0, 0, 0, 0); // No margins
            tv.setLayoutParams(params);
            
            tv.setText(text.toUpperCase());
            tv.setTextSize(10);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.WHITE);
            tv.setPadding(dpToPx(context, 10), 0, dpToPx(context, 10), 0);
            tv.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            tv.setSingleLine(true);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            
            // Header background with border
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.parseColor("#5C6BC0")); // Indigo
            border.setStroke(dpToPx(context, 1), Color.parseColor("#3F51B5")); // Darker indigo border
            tv.setBackground(border);
            
            return tv;
        }
    }

    class RowViewHolder extends RecyclerView.ViewHolder {
        LinearLayout rowContainer;

        RowViewHolder(View itemView) {
            super(itemView);
            rowContainer = itemView.findViewById(R.id.rowContainer);
        }

        void bind(List<String> columns, Map<String, Object> row, int position) {
            rowContainer.removeAllViews();
            Context context = rowContainer.getContext();
            
            // Alternating row colors
            int bgColor = (position % 2 == 0) ? Color.WHITE : Color.parseColor("#F8F9FA");
            
            for (String column : columns) {
                Object value = row.get(column);
                TextView tv = createDataCell(context, value, bgColor);
                rowContainer.addView(tv);
            }
            
            // Delete button cell
            LinearLayout actionCell = createActionCell(context, row, bgColor);
            rowContainer.addView(actionCell);
        }

        private TextView createDataCell(Context context, Object value, int bgColor) {
            TextView tv = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(context, COLUMN_WIDTH_DP),
                dpToPx(context, 48) // Fixed height to match header
            );
            params.setMargins(0, 0, 0, 0); // No margins
            tv.setLayoutParams(params);
            
            tv.setPadding(dpToPx(context, 10), 0, dpToPx(context, 10), 0);
            tv.setTextSize(12);
            tv.setMaxLines(2);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tv.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            
            if (value == null) {
                tv.setText("null");
                tv.setTextColor(Color.parseColor("#9E9E9E"));
                tv.setTypeface(null, Typeface.ITALIC);
                tv.setTextSize(11);
            } else if (value instanceof Boolean) {
                tv.setText(value.toString());
                boolean boolValue = (Boolean) value;
                tv.setTextColor(Color.WHITE);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setTextSize(11);
                tv.setGravity(android.view.Gravity.CENTER);
                
                // Badge background
                GradientDrawable badge = new GradientDrawable();
                badge.setCornerRadius(dpToPx(context, 6));
                badge.setColor(boolValue ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
                badge.setStroke(dpToPx(context, 1), Color.parseColor("#E0E0E0"));
                tv.setBackground(badge);
                tv.setPadding(dpToPx(context, 12), dpToPx(context, 6), dpToPx(context, 12), dpToPx(context, 6));
                return tv;
            } else if (value instanceof Number) {
                tv.setText(value.toString());
                tv.setTextColor(Color.parseColor("#1976D2")); // Blue for numbers
                tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            } else {
                String strValue = value.toString();
                tv.setText(strValue);
                tv.setTextColor(Color.parseColor("#212121"));
                tv.setTypeface(Typeface.MONOSPACE);
            }
            
            // Cell border
            GradientDrawable border = new GradientDrawable();
            border.setColor(bgColor);
            border.setStroke(dpToPx(context, 1), Color.parseColor("#DDDDDD"));
            tv.setBackground(border);
            
            return tv;
        }

        private LinearLayout createActionCell(Context context, Map<String, Object> row, int bgColor) {
            LinearLayout actionCell = new LinearLayout(context);
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                dpToPx(context, ACTION_COLUMN_WIDTH_DP),
                dpToPx(context, 48) // Fixed height to match header
            );
            cellParams.setMargins(0, 0, 0, 0); // No margins
            actionCell.setLayoutParams(cellParams);
            actionCell.setOrientation(LinearLayout.VERTICAL);
            actionCell.setGravity(android.view.Gravity.CENTER);
            actionCell.setPadding(0, 0, 0, 0);
            
            // Border
            GradientDrawable border = new GradientDrawable();
            border.setColor(bgColor);
            border.setStroke(dpToPx(context, 1), Color.parseColor("#DDDDDD"));
            actionCell.setBackground(border);
            
            // Delete button
            ImageView deleteBtn = new ImageView(context);
            deleteBtn.setImageResource(R.drawable.ic_delete);
            deleteBtn.setColorFilter(Color.WHITE);
            
            int size = dpToPx(context, 32);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(size, size);
            deleteBtn.setLayoutParams(btnParams);
            deleteBtn.setPadding(dpToPx(context, 6), dpToPx(context, 6), 
                                dpToPx(context, 6), dpToPx(context, 6));
            
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor("#F44336")); // Red background
            deleteBtn.setBackground(circle);
            
            deleteBtn.setClickable(true);
            deleteBtn.setFocusable(true);
            
            deleteBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(row);
                }
            });
            
            actionCell.addView(deleteBtn);
            return actionCell;
        }
    }
}
