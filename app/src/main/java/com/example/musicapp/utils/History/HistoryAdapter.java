package com.example.musicapp.utils.History;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.example.musicapp.PlayHistory;
import com.example.musicapp.databinding.ItemHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends ArrayAdapter<PlayHistory> {

    public HistoryAdapter(Context context, List<PlayHistory> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemHistoryBinding binding;

        if (convertView == null) {
            binding = ItemHistoryBinding.inflate(LayoutInflater.from(getContext()), parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (ItemHistoryBinding) convertView.getTag();
        }

        PlayHistory item = getItem(position);
        if (item != null) {
            binding.tvSongTitle.setText(item.getSongTitle());
            binding.tvArtist.setText(item.getArtist());
            binding.tvPlayedAt.setText(formatTime(item.getPlayedAt()));
            binding.tvPlayCount.setText("Lượt nghe: " + item.getPlayCount());
        }

        return convertView;
    }

    private String formatTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(time));
    }
}