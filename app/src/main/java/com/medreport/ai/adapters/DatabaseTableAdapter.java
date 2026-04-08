package com.medreport.ai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.models.ResponseModels;
import java.util.ArrayList;
import java.util.List;

public class DatabaseTableAdapter extends RecyclerView.Adapter<DatabaseTableAdapter.ViewHolder> {

    private List<ResponseModels.DatabaseTable> tables = new ArrayList<>();
    private OnTableClickListener listener;
    private String selectedTableName;

    public interface OnTableClickListener {
        void onTableClick(ResponseModels.DatabaseTable table);
    }

    public DatabaseTableAdapter(OnTableClickListener listener) {
        this.listener = listener;
    }

    public void setTables(List<ResponseModels.DatabaseTable> tables) {
        this.tables = tables != null ? tables : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedTable(String tableName) {
        this.selectedTableName = tableName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_database_table_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResponseModels.DatabaseTable table = tables.get(position);
        holder.bind(table, table.name.equals(selectedTableName));
    }

    @Override
    public int getItemCount() {
        return tables.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTableName, tvRowCount;
        View container, cardContent;

        ViewHolder(View itemView) {
            super(itemView);
            tvTableName = itemView.findViewById(R.id.tvTableName);
            tvRowCount = itemView.findViewById(R.id.tvRowCount);
            container = itemView.findViewById(R.id.container);
            cardContent = itemView.findViewById(R.id.cardContent);
        }

        void bind(ResponseModels.DatabaseTable table, boolean isSelected) {
            tvTableName.setText(table.name);
            tvRowCount.setText(table.rowCount + " rows");
            
            cardContent.setSelected(isSelected);
            container.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTableClick(table);
                }
            });
        }
    }
}
