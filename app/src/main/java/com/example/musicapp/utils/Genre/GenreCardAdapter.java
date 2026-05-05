package com.example.musicapp.utils.Genre;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.R;
import com.example.musicapp.model.GenreCard;

import java.util.ArrayList;
import java.util.List;

public class GenreCardAdapter extends RecyclerView.Adapter<GenreCardAdapter.ViewHolder> {

    public interface OnGenreClickListener {
        void onGenreClick(GenreCard genreCard);
    }

    private final List<GenreCard> items = new ArrayList<>();
    private final OnGenreClickListener listener;

    public GenreCardAdapter(OnGenreClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<GenreCard> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GenreCard item = items.get(position);
        holder.textGenreName.setText(item.getGenreName());
        holder.textGenreCount.setText(item.getSongCount() + " bài hát");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGenreClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textGenreName;
        TextView textGenreCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textGenreName = itemView.findViewById(R.id.textGenreName);
            textGenreCount = itemView.findViewById(R.id.textGenreCount);
        }
    }
}