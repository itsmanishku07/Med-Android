package com.medreport.ai.adapters;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.models.ReminderModel;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.VH> {
    public interface Listener { void onEdit(ReminderModel r); void onDelete(ReminderModel r); void onToggle(ReminderModel r); }
    private final List<ReminderModel> items;
    private final Listener listener;

    public ReminderAdapter(List<ReminderModel> items, Listener listener) { this.items = items; this.listener = listener; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ReminderModel r = items.get(pos);
        h.tvName.setText(r.medicineName);
        h.tvTime.setText(r.getDisplayTime());
        h.tvDosage.setText(r.dosage != null ? r.dosage : "");
        h.tvDays.setText(r.getDaysDisplay());
        h.tvNotes.setText(r.notes != null ? r.notes : "");
        h.tvNotes.setVisibility(r.notes != null && !r.notes.isEmpty() ? View.VISIBLE : View.GONE);
        h.switchActive.setChecked(r.isActive);
        h.switchActive.setOnCheckedChangeListener(null);
        h.switchActive.setOnCheckedChangeListener((sw, checked) -> listener.onToggle(r));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(r));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(r));
        h.itemView.setAlpha(r.isActive ? 1f : 0.5f);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvDosage, tvDays, tvNotes;
        androidx.appcompat.widget.SwitchCompat switchActive;
        ImageButton btnEdit, btnDelete;
        VH(View v) {
            super(v);
            tvName     = v.findViewById(R.id.tvMedicineName);
            tvTime     = v.findViewById(R.id.tvTime);
            tvDosage   = v.findViewById(R.id.tvDosage);
            tvDays     = v.findViewById(R.id.tvDays);
            tvNotes    = v.findViewById(R.id.tvNotes);
            switchActive = v.findViewById(R.id.switchActive);
            btnEdit    = v.findViewById(R.id.btnEdit);
            btnDelete  = v.findViewById(R.id.btnDelete);
        }
    }
}
