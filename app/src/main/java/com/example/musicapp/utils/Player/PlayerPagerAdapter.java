package com.example.musicapp.utils.Player;

import android.animation.ObjectAnimator;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.databinding.ItemPlayerArtPageBinding;
import com.example.musicapp.databinding.ItemPlayerLyricsPageBinding;
import com.example.musicapp.model.LyricLine;
import com.example.musicapp.model.Song;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import java.util.ArrayList;
import java.util.List;

public class PlayerPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ART = 0;
    private static final int TYPE_LYRICS = 1;

    private final Context context;
    private Song currentSong;
    private List<LyricLine> lyricLines = new ArrayList<>();

    private ArtHolder artHolder;
    private LyricsHolder lyricsHolder;
    private ObjectAnimator armAnimator;

    public PlayerPagerAdapter(Context context) {
        this.context = context;
    }

    public void setSong(Song song) {
        currentSong = song;
        notifyItemChanged(TYPE_ART);
    }

    private boolean isPlaying = false;

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        notifyItemChanged(TYPE_ART);
    }

    public void setLyrics(List<LyricLine> lines) {
        lyricLines = lines != null ? lines : new ArrayList<>();
        notifyItemChanged(TYPE_LYRICS);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_ART : TYPE_LYRICS;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ART) {
            ItemPlayerArtPageBinding binding = ItemPlayerArtPageBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ArtHolder(binding);
        } else {
            ItemPlayerLyricsPageBinding binding = ItemPlayerLyricsPageBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new LyricsHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ArtHolder) {
            artHolder = (ArtHolder) holder;
            bindArtHolder(artHolder);
        } else if (holder instanceof LyricsHolder) {
            lyricsHolder = (LyricsHolder) holder;
            bindLyricsHolder(lyricsHolder);
        }
    }

    private void bindArtHolder(ArtHolder holder) {
        if (currentSong == null) return;

        holder.binding.pageTitle.setText(currentSong.title != null ? currentSong.title : "");
        holder.binding.pageArtist.setText(currentSong.artist != null ? currentSong.artist : "");

        Bitmap embeddedArt = getEmbeddedAlbumArt(currentSong.data);

        if (embeddedArt != null) {
            holder.binding.pageAlbumArt.setImageBitmap(embeddedArt);
        } else if (currentSong.albumId > 0) {
            Uri albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    currentSong.albumId
            );

            Glide.with(context)
                    .clear(holder.binding.pageAlbumArt);

            Glide.with(context)
                    .load(albumArtUri)
                    .placeholder(R.drawable.ic_music_note_24)
                    .error(R.drawable.ic_music_note_24)
                    .into(holder.binding.pageAlbumArt);
        } else {
            holder.binding.pageAlbumArt.setImageResource(R.drawable.ic_music_note_24);
        }
        if (isPlaying) {
            startDiscRotation(holder.binding.pageAlbumArt);
            moveArmToPlay(holder.binding.imageTurntableArm);
        } else {
            stopDiscRotation();
            moveArmToPause(holder.binding.imageTurntableArm);
        }
    }

    private void bindLyricsHolder(LyricsHolder holder) {
        holder.binding.lyricsContainer.removeAllViews();

        for (LyricLine line : lyricLines) {
            TextView tv = new TextView(context);
            tv.setText(line.text == null || line.text.trim().isEmpty() ? "♪" : line.text);
            tv.setTextSize(18f);
            tv.setTextColor(0x66FFFFFF);
            tv.setPadding(0, 20, 0, 20);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            holder.binding.lyricsContainer.addView(tv);
        }
    }

    public void highlightLyric(long currentTime) {
        if (lyricsHolder == null || lyricLines.isEmpty()) return;

        int index = -1;
        for (int i = 0; i < lyricLines.size(); i++) {
            if (currentTime >= lyricLines.get(i).time) {
                index = i;
            } else {
                break;
            }
        }

        int childCount = lyricsHolder.binding.lyricsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            TextView tv = (TextView) lyricsHolder.binding.lyricsContainer.getChildAt(i);
            if (i == index) {
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(24f);
                tv.setAlpha(1f);
            } else if (Math.abs(i - index) == 1) {
                tv.setTextColor(0xCCFFFFFF);
                tv.setTextSize(20f);
                tv.setAlpha(0.8f);
            } else {
                tv.setTextColor(0x66FFFFFF);
                tv.setTextSize(18f);
                tv.setAlpha(0.45f);
            }
        }

        if (index >= 0 && index < childCount) {
            TextView current = (TextView) lyricsHolder.binding.lyricsContainer.getChildAt(index);
            lyricsHolder.binding.lyricsScroll.smoothScrollTo(0, Math.max(0, current.getTop() - 250));
        }
    }

    static class ArtHolder extends RecyclerView.ViewHolder {
        final ItemPlayerArtPageBinding binding;

        ArtHolder(ItemPlayerArtPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class LyricsHolder extends RecyclerView.ViewHolder {
        final ItemPlayerLyricsPageBinding binding;

        LyricsHolder(ItemPlayerLyricsPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
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

    private ObjectAnimator rotationAnimator;

    private void startDiscRotation(View view) {
        stopDiscRotation();

        rotationAnimator = ObjectAnimator.ofFloat(
                view,
                "rotation",
                view.getRotation(),
                view.getRotation() + 360f
        );
        rotationAnimator.setDuration(12000);
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.start();
    }

    private void stopDiscRotation() {
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
    }

    private void moveArmToPlay(View armView) {
        if (armAnimator != null) {
            armAnimator.cancel();
        }
        armAnimator = ObjectAnimator.ofFloat(armView, "rotation", armView.getRotation(), -8f);
        armAnimator.setDuration(350);
        armAnimator.start();
    }

    private void moveArmToPause(View armView) {
        if (armAnimator != null) {
            armAnimator.cancel();
        }
        armAnimator = ObjectAnimator.ofFloat(armView, "rotation", armView.getRotation(), -28f);
        armAnimator.setDuration(350);
        armAnimator.start();
    }
}