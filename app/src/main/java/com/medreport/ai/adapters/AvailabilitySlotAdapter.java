package com.medreport.ai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.R;
import com.medreport.ai.databinding.ItemAvailabilitySlotBinding;
import com.medreport.ai.models.AvailabilitySlot;
import java.util.List;

public class AvailabilitySlotAdapter extends RecyclerView.Adapter<AvailabilitySlotAdapter.VH> {
    public interface Listener {
        void onEdit(AvailabilitySlot slot);
        void onDelete(AvailabilitySlot slot);
        void onToggle(AvailabilitySlot slot);
    }

    private final List<AvailabilitySlot> items;
    private final Listener listener;

    public AvailabilitySlotAdapter(List<AvailabilitySlot> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAvailabilitySlotBinding binding = ItemAvailabilitySlotBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        private final ItemAvailabilitySlotBinding b;

        VH(ItemAvailabilitySlotBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(AvailabilitySlot slot) {
            b.tvDayOfWeek.setText(slot.getDayDisplay());
            b.tvTimeRange.setText(slot.getTimeRange());
            
            if (b.getRoot().findViewById(R.id.switchAvailable) != null) {
                b.switchAvailable.setChecked(slot.isAvailable != null && slot.isAvailable);
                
                b.switchAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (buttonView.isPressed()) {
                        listener.onToggle(slot);
                    }
                });
            }

            b.btnDelete.setOnClickListener(v -> listener.onDelete(slot));
        }
    }
}
