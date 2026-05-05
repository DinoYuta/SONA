package com.example.musicapp.utils.Search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.databinding.ItemRecentSearchBinding;

import java.util.ArrayList;
import java.util.List;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.ViewHolder> {

    public interface OnRecentClickListener {
        void onRecentClick(String query);
        void onRemoveClick(String query);
    }

    private final List<String> items = new ArrayList<>();
    private final OnRecentClickListener listener;

    public RecentSearchAdapter(OnRecentClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<String> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecentSearchBinding binding = ItemRecentSearchBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = items.get(position);
        holder.bind(query);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentSearchBinding binding;

        ViewHolder(ItemRecentSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String query) {
            binding.textRecentQuery.setText(query);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onRecentClick(query);
            });

            binding.btnRemoveRecent.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveClick(query);
            });
        }
    }
}