package com.example.musicapp.utils.Player;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.databinding.ActivityPlayerBinding;
import com.example.musicapp.model.LyricLine;
import com.example.musicapp.model.Song;
import com.example.musicapp.utils.Lyric.LrcParser;
import com.example.musicapp.utils.Music.MusicPlayerManager;
import com.example.musicapp.utils.Playlist.PlaylistHelper;
import com.frolo.waveformseekbar.WaveformSeekBar;
import androidx.media3.common.Player;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;

import com.example.musicapp.SessionManager;

import java.text.Normalizer;
import java.util.Locale;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class PlayerActivity extends AppCompatActivity implements MusicPlayerManager.PlaybackListener {

    private ActivityPlayerBinding binding;
    private MusicPlayerManager playerManager;
    private PlayerPagerAdapter pagerAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<LyricLine> lyricLines = new ArrayList<>();
    private PlaylistHelper playlistHelper;
    private SessionManager sessionManager;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Player player = playerManager != null ? playerManager.getPlayer() : null;
            if (player != null) {
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();

                if (duration > 0) {
                    float progressPercent = ((float) currentPosition / duration);
                    binding.waveformSeekBar3.setProgressInPercentage(progressPercent);
                    binding.textElapsed.setText(formatTime((int) (currentPosition / 1000)));
                    binding.textDuration.setText(formatTime((int) (duration / 1000)));
                }

                if (pagerAdapter != null) {
                    pagerAdapter.highlightLyric(currentPosition);
                }
            }
            handler.postDelayed(this, 350);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        playerManager = MusicPlayerManager.getInstance(this);
        playerManager.addListener(this);
        playlistHelper = new PlaylistHelper(this);
        sessionManager = new SessionManager(this);

        if (!playerManager.hasPlaylist()) {
            Toast.makeText(this, "No Songs Found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pagerAdapter = new PlayerPagerAdapter(this);
        binding.playerPager.setAdapter(pagerAdapter);
        binding.playerPager.setOffscreenPageLimit(2);
        binding.playerPager.setCurrentItem(0, false);

        binding.waveformSeekBar3.setWaveform(createWaveForm(), true);

        loadLyrics();
        setupControls();
        syncUI();
        setupFavButton();

        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void setupControls() {
        binding.buttonPlayPause.setOnClickListener(v -> {
            playerManager.togglePlayPause();
            syncUI();
        });

        binding.buttonNext.setOnClickListener(v -> {
            playerManager.playNext();
            loadLyrics();
            syncUI();
        });

        binding.buttonPrev.setOnClickListener(v -> {
            playerManager.playPrevious();
            loadLyrics();
            syncUI();
        });

        binding.waveformSeekBar3.setCallback(new WaveformSeekBar.Callback() {
            @Override
            public void onProgressChanged(WaveformSeekBar seekBar, float percent, boolean fromUser) {
                Player player = playerManager.getPlayer();
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    long seekPos = (long) (percent * duration);
                    player.seekTo(seekPos);
                    playerManager.persistPlaybackState();
                    binding.textElapsed.setText(formatTime((int) (seekPos / 1000)));
                    if (pagerAdapter != null) {
                        pagerAdapter.highlightLyric(seekPos);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(WaveformSeekBar seekBar) {
                handler.removeCallbacks(updateRunnable);
            }

            @Override
            public void onStopTrackingTouch(WaveformSeekBar seekBar) {
                handler.post(updateRunnable);
            }
        });

        binding.buttonShuffle.setOnClickListener(v ->
                Toast.makeText(this, "Shuffle sẽ làm sau", Toast.LENGTH_SHORT).show());

        binding.buttonRepeat.setOnClickListener(v -> {
            Player player = playerManager.getPlayer();
            if (player == null) return;

            int repeatMode = player.getRepeatMode();
            if (repeatMode == Player.REPEAT_MODE_ONE) {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                binding.buttonRepeat.clearColorFilter();
            } else {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                binding.buttonRepeat.setColorFilter(getResources().getColor(R.color.purple));
            }
        });
    }

    private void syncUI() {
        Song song = playerManager.getCurrentSong();
        if (song == null) return;

        updateBackground(song);

        if (pagerAdapter != null) {
            pagerAdapter.setPlaying(playerManager.isPlaying());
            pagerAdapter.setSong(song);
            pagerAdapter.setLyrics(lyricLines);
        }

        updatePlayPauseButtonIcon();
        updateFavState();

        Player player = playerManager.getPlayer();
        if (player == null) return;

        long duration = player.getDuration();
        if (duration > 0) {
            binding.textDuration.setText(formatTime((int) (duration / 1000)));
        }

        handler.removeCallbacks(updateRunnable);
        handler.post(updateRunnable);
    }

    private void updateBackground(Song song) {
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();

        try {
            retriever.setDataSource(song.data);
            byte[] artBytes = retriever.getEmbeddedPicture();

            if (artBytes != null) {
                Glide.with(this)
                        .asBitmap()
                        .load(artBytes)
                        .apply(bitmapTransform(new BlurTransformation(25, 3)))
                        .placeholder(R.drawable.ic_music_note_24)
                        .error(R.drawable.ic_music_note_24)
                        .into(binding.bgAlbumArt);

            } else {
                Uri albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        song.albumId
                );

                Glide.with(this)
                        .load(albumArtUri)
                        .apply(bitmapTransform(new BlurTransformation(25, 3)))
                        .placeholder(R.drawable.ic_music_note_24)
                        .error(R.drawable.ic_music_note_24)
                        .into(binding.bgAlbumArt);

            }
        } catch (Exception e) {
            binding.bgAlbumArt.setImageResource(R.drawable.ic_music_note_24);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private void loadLyrics() {
        Song currentSong = playerManager.getCurrentSong();

        if (currentSong == null) {
            lyricLines = new ArrayList<>();
            if (pagerAdapter != null) {
                pagerAdapter.setLyrics(lyricLines);
            }
            return;
        }

        String lyricFileName = getLyricFileName(currentSong);

        if (lyricFileName == null || lyricFileName.trim().isEmpty()) {
            lyricLines = new ArrayList<>();
            if (pagerAdapter != null) {
                pagerAdapter.setLyrics(lyricLines);
            }
            return;
        }

        try (InputStream is = getAssets().open(lyricFileName)) {
            lyricLines = LrcParser.parse(is);

            if (pagerAdapter != null) {
                pagerAdapter.setLyrics(lyricLines);
            }

            Toast.makeText(this, "Đã tải lyric: " + lyricFileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            lyricLines = new ArrayList<>();

            if (pagerAdapter != null) {
                pagerAdapter.setLyrics(lyricLines);
            }

            Toast.makeText(this, "Không tìm thấy lyric cho: " + currentSong.title, Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private int[] createWaveForm() {
        Random random = new Random(System.currentTimeMillis());
        int[] values = new int[50];
        for (int i = 0; i < values.length; i++) {
            values[i] = 5 + random.nextInt(50);
        }
        return values;
    }

    private void updatePlayPauseButtonIcon() {
        binding.buttonPlayPause.setImageResource(
                playerManager.isPlaying()
                        ? R.drawable.ic_pause_24
                        : R.drawable.ic_play_arrow_24
        );
    }

    private void animateFav(boolean isAdded) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.favBtn, "scaleX", 1f, 1.25f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.favBtn, "scaleY", 1f, 1.25f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.setDuration(220);
        set.playTogether(scaleX, scaleY);
        set.start();

        binding.favBtn.setColorFilter(isAdded ? Color.RED : Color.WHITE);
    }

    private void updateFavState() {
        Song song = playerManager.getCurrentSong();
        if (song == null) return;

        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            binding.favBtn.setColorFilter(Color.WHITE);
            return;
        }

        boolean exists = playlistHelper.isSongInPlaylist(
                sessionManager.getUserId(),
                song.data
        );

        binding.favBtn.setColorFilter(exists ? Color.RED : Color.WHITE);
    }

    private void setupFavButton() {
        binding.favBtn.setOnClickListener(v -> {
            Song currentSong = playerManager.getCurrentSong();
            if (currentSong == null) return;

            if (sessionManager == null || !sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean exists = playlistHelper.isSongInPlaylist(
                    sessionManager.getUserId(),
                    currentSong.data
            );

            if (exists) {
                animateFav(true);
                Toast.makeText(this, "Bài này đã có trong playlist", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean added = playlistHelper.addSongToPlaylist(
                    sessionManager.getUserId(),
                    currentSong
            );

            if (added) {
                animateFav(true);
                Toast.makeText(this, "Đã thêm vào playlist", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không thể thêm vào playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String normalizeLyricFileName(String text) {
        if (text == null) return "";

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        normalized = normalized.toLowerCase(Locale.getDefault())
                .replaceAll("[^a-z0-9]", "");

        return normalized;
    }

    private String getLyricFileName(Song song) {
        if (song == null) return null;

        String title = song.title != null ? song.title.trim() : "";
        if (!title.isEmpty()) {
            return normalizeLyricFileName(title) + ".lrc";
        }

        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerManager != null) {
            playerManager.persistPlaybackState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        if (playerManager != null) {
            playerManager.removeListener(this);
        }
    }

    @Override
    public void onPlaybackStateChanged() {
        runOnUiThread(() -> {
            loadLyrics();
            syncUI();
        });
    }
}