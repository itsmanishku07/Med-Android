package com.medreport.ai.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medreport.ai.databinding.ItemBlockedDateBinding;
import com.medreport.ai.models.BlockedDate;
import java.util.List;

public class BlockedDateAdapter extends RecyclerView.Adapter<BlockedDateAdapter.VH> {
    public interface Listener {
        void onDelete(BlockedDate blockedDate);
    }

    private final List<BlockedDate> items;
    private final Listener listener;

    public BlockedDateAdapter(List<BlockedDate> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBlockedDateBinding binding = ItemBlockedDateBinding.inflate(
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
        private final ItemBlockedDateBinding b;

        VH(ItemBlockedDateBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(BlockedDate blockedDate) {
            b.tvBlockedDate.setText(blockedDate.getDisplayDate());
            b.tvReason.setText(blockedDate.reason != null && !blockedDate.reason.isEmpty() 
                    ? blockedDate.reason : "No reason specified");
            b.btnDelete.setOnClickListener(v -> listener.onDelete(blockedDate));
        }
    }
}
