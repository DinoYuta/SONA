package com.example.musicapp.utils;

import android.content.ContentUris;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.databinding.ItemSongBinding;
import com.example.musicapp.model.Song;
import com.example.musicapp.model.SongStat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewholder> {

    private final List<Song> songs = new ArrayList<>();
    private final OnItemClickListerner listener;
    private final Map<String, Integer> playCountMap = new HashMap<>();
    private final Map<String, Integer> oldPlayCountMap = new HashMap<>();
    public interface OnItemClickListerner {
        void onItemClick(int position);
    }

    public SongAdapter(List<Song> songs, OnItemClickListerner listener) {
        if (songs != null) {
            this.songs.addAll(songs);
        }
        this.listener = listener;
    }

    public void updateSongs(List<Song> newSongs) {
        songs.clear();
        if (newSongs != null) {
            songs.addAll(newSongs);
        }
    }

    private Bitmap getEmbeddedAlbumArt(String songPath) {
        if (songPath == null || songPath.trim().isEmpty()) return null;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(songPath);
            byte[] artBytes = retriever.getEmbeddedPicture();
            if (artBytes != null && artBytes.length > 0) {
                return BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @NonNull
    @Override
    public SongViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSongBinding binding = ItemSongBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SongViewholder(binding, listener);
    }


    @Override
    public void onBindViewHolder(@NonNull SongViewholder holder, int position) {
        Song song = songs.get(position);

        holder.binding.textTitle.setText(song.title != null ? song.title : "");
        holder.binding.textArtist.setText(song.artist != null ? song.artist : "");

        int newPlayCount = playCountMap.getOrDefault(song.data, 0);
        int oldPlayCount = oldPlayCountMap.getOrDefault(song.data, newPlayCount);

        holder.binding.tvPlayCount.setText("👁 " + formatPlayCount(newPlayCount));

        if (newPlayCount > oldPlayCount) {
            animatePlayCount(holder.binding.tvPlayCount);
        }

        Bitmap embeddedArt = getEmbeddedAlbumArt(song.data);

        if (embeddedArt != null) {
            holder.binding.imageAlbumArt.setImageBitmap(embeddedArt);
        } else if (song.albumId > 0) {
            Uri albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    song.albumId
            );

            Glide.with(holder.binding.getRoot().getContext()).clear(holder.binding.imageAlbumArt);

            Glide.with(holder.binding.getRoot().getContext())
                    .load(albumArtUri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_music_note_24)
                    .error(R.drawable.ic_music_note_24)
                    .into(holder.binding.imageAlbumArt);
        } else {
            holder.binding.imageAlbumArt.setImageResource(R.drawable.ic_music_note_24);
        }
    }

    private String formatPlayCount(int count) {
        if (count >= 1000000) {
            return (count / 1000000) + "M";
        } else if (count >= 1000) {
            return (count / 1000) + "K";
        } else {
            return String.valueOf(count);
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewholder extends RecyclerView.ViewHolder {
        final ItemSongBinding binding;

        public SongViewholder(ItemSongBinding binding, OnItemClickListerner listener) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            listener.onItemClick(pos);
                        }
                    }
                }
            });
        }
    }

    public void updatePlayCounts(List<SongStat> stats) {
        oldPlayCountMap.clear();
        oldPlayCountMap.putAll(playCountMap);

        playCountMap.clear();

        if (stats != null) {
            for (SongStat stat : stats) {
                if (stat.getSongPath() != null) {
                    playCountMap.put(stat.getSongPath(), stat.getTotalPlayCount());
                }
            }
        }
        notifyDataSetChanged();
    }

    private void animatePlayCount(TextView view) {
        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.12f);
        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.12f);
        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(view, "scaleX", 1.12f, 1f);
        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(view, "scaleY", 1.12f, 1f);

        scaleXUp.setDuration(120);
        scaleYUp.setDuration(120);
        scaleXDown.setDuration(120);
        scaleYDown.setDuration(120);

        AnimatorSet upSet = new AnimatorSet();
        upSet.playTogether(scaleXUp, scaleYUp);

        AnimatorSet downSet = new AnimatorSet();
        downSet.playTogether(scaleXDown, scaleYDown);

        AnimatorSet fullSet = new AnimatorSet();
        fullSet.playSequentially(upSet, downSet);
        fullSet.start();
    }
}