package com.example.musicapp.utils.Playlist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.AppDatabaseHelper;
import com.example.musicapp.R;
import com.example.musicapp.SessionManager;
import com.example.musicapp.databinding.ActivityPlaylistBinding;
import com.example.musicapp.model.Song;

import com.example.musicapp.utils.Music.MusicPlayerManager;
import com.example.musicapp.utils.Player.PlayerActivity;
import com.example.musicapp.utils.Search.SearchActivity;
import com.example.musicapp.utils.SongAdapter;
import com.example.musicapp.utils.SongStatsHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.musicapp.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity
        implements SongAdapter.OnItemClickListerner, MusicPlayerManager.PlaybackListener {

    private ActivityPlaylistBinding binding;
    private RecyclerView recyclerViewPlaylist;
    private TextView textPlaylistInfo;

    private SongAdapter adapter;
    private final List<Song> playlistSongs = new ArrayList<>();

    private SessionManager sessionManager;
    private PlaylistHelper playlistHelper;
    private MusicPlayerManager playerManager;
    private AppDatabaseHelper dbHelper;
    private SessionManager session;
    private SongStatsHelper songStatsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPlaylistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        recyclerViewPlaylist = binding.recyclerViewPlaylist;
        textPlaylistInfo = binding.textPlaylistInfo;

        sessionManager = new SessionManager(this);
        playlistHelper = new PlaylistHelper(this);
        playerManager = MusicPlayerManager.getInstance(this);
        playerManager.addListener(this);

        dbHelper = new AppDatabaseHelper(this);
        songStatsHelper = new SongStatsHelper(this);

        recyclerViewPlaylist.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(playlistSongs, this);
        recyclerViewPlaylist.setAdapter(adapter);

        BottomNavigationView bottomNav = binding.bottomNav;
        bottomNav.setSelectedItemId(R.id.nav_library);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            if (id == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            if (id == R.id.nav_library) {
                return true;
            }

            return false;
        });

        setupMiniPlayer();
    }

    private void loadPlaylist() {
        playlistSongs.clear();

        if (sessionManager != null && sessionManager.isLoggedIn()) {
            playlistSongs.addAll(playlistHelper.getPlaylistSongs(sessionManager.getUserId()));
        }

        adapter.updateSongs(new ArrayList<>(playlistSongs));
        adapter.updatePlayCounts(songStatsHelper.getTopTrendingSongs(1000));
        textPlaylistInfo.setText(playlistSongs.size() + " bài hát");
    }

    private void setupMiniPlayer() {
        binding.miniPlayer.setOnClickListener(v -> {
            if (!playerManager.hasPlaylist()) return;
            startActivity(new Intent(this, PlayerActivity.class));
        });

        binding.btnMiniPlay.setOnClickListener(v -> {
            playerManager.togglePlayPause();
            syncMiniPlayer();
        });

        binding.btnPrev.setOnClickListener(v -> {
            playerManager.playPrevious();
            syncMiniPlayer();
            saveCurrentSongToHistory();
        });

        binding.btnNext.setOnClickListener(v -> {
            playerManager.playNext();
            syncMiniPlayer();
            saveCurrentSongToHistory();
        });
    }

    private void saveCurrentSongToHistory() {
        Song currentSong = playerManager.getCurrentSong();
        if (currentSong == null) return;

        if (currentSong.data == null || currentSong.data.trim().isEmpty()) return;

        java.io.File audioFile = new java.io.File(currentSong.data);
        if (!audioFile.exists()) return;

        if (sessionManager != null && sessionManager.isLoggedIn()) {
            dbHelper.savePlayHistory(
                    sessionManager.getUserId(),
                    currentSong.title != null ? currentSong.title : "Unknown title",
                    currentSong.artist != null ? currentSong.artist : "Unknown artist",
                    currentSong.data
            );
        }

        songStatsHelper.incrementPlayCount(
                currentSong.title,
                currentSong.artist,
                currentSong.data
        );

        adapter.updatePlayCounts(songStatsHelper.getTopTrendingSongs(1000));
    }

    private void syncMiniPlayer() {
        Song currentSong = playerManager.getCurrentSong();

        if (currentSong == null) {
            binding.miniPlayer.setVisibility(View.GONE);
            return;
        }

        binding.miniPlayer.setVisibility(View.VISIBLE);
        binding.miniTitle.setText(currentSong.title != null ? currentSong.title : "");
        binding.miniArtist.setText(currentSong.artist != null ? currentSong.artist : "");

        binding.btnMiniPlay.setImageResource(
                playerManager.isPlaying()
                        ? R.drawable.ic_pause_24
                        : R.drawable.ic_play_arrow_24
        );

        android.graphics.Bitmap embeddedArt = getEmbeddedAlbumArt(currentSong.data);
        if (embeddedArt != null) {
            binding.miniAlbumArt.setImageBitmap(embeddedArt);
        } else {
            binding.miniAlbumArt.setImageResource(R.drawable.ic_music_note_24);
        }
    }

    private android.graphics.Bitmap getEmbeddedAlbumArt(String songPath) {
        if (songPath == null || songPath.trim().isEmpty()) return null;

        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {
            retriever.setDataSource(songPath);
            byte[] artBytes = retriever.getEmbeddedPicture();
            if (artBytes != null && artBytes.length > 0) {
                return android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylist();
        syncMiniPlayer();
    }

    @Override
    public void onPlaybackStateChanged() {
        runOnUiThread(this::syncMiniPlayer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerManager != null) {
            playerManager.removeListener(this);
        }
    }

    @Override
    public void onItemClick(int position) {
        if (position < 0 || position >= playlistSongs.size()) return;

        Song selectedSong = playlistSongs.get(position);

        if (selectedSong.data == null || selectedSong.data.trim().isEmpty()) return;

        java.io.File audioFile = new java.io.File(selectedSong.data);
        if (!audioFile.exists()) return;

        if (sessionManager != null && sessionManager.isLoggedIn()) {
            dbHelper.savePlayHistory(
                    sessionManager.getUserId(),
                    selectedSong.title != null ? selectedSong.title : "Unknown title",
                    selectedSong.artist != null ? selectedSong.artist : "Unknown artist",
                    selectedSong.data
            );
        }

        songStatsHelper.incrementPlayCount(
                selectedSong.title,
                selectedSong.artist,
                selectedSong.data
        );

        playerManager.setPlaylist(new ArrayList<>(playlistSongs), position);
        adapter.updatePlayCounts(songStatsHelper.getTopTrendingSongs(1000));
        startActivity(new Intent(this, PlayerActivity.class));
    }
}