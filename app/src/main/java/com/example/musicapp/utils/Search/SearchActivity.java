package com.example.musicapp.utils.Search;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicapp.R;
import com.example.musicapp.databinding.ActivitySearchBinding;
import com.example.musicapp.model.Song;
import com.example.musicapp.utils.Lyric.LyricSearchUtils;
import com.example.musicapp.utils.Music.MusicPlayerManager;
import com.example.musicapp.utils.Player.PlayerActivity;

import com.example.musicapp.utils.Playlist.PlaylistActivity;
import com.example.musicapp.utils.SongAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.musicapp.MainActivity;
import com.google.android.material.button.MaterialButton;

import com.example.musicapp.AppDatabaseHelper;
import com.example.musicapp.SessionManager;
import com.example.musicapp.utils.SongStatsHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchActivity extends AppCompatActivity
        implements SongAdapter.OnItemClickListerner, MusicPlayerManager.PlaybackListener {

    private ActivitySearchBinding binding;
    private SongAdapter songAdapter;
    private RecentSearchAdapter recentSearchAdapter;
    private SearchHistoryManager searchHistoryManager;
    private MusicPlayerManager playerManager;
    private String selectedGenre = "Tất cả";
    private final List<Song> allSongs = new ArrayList<>();
    private final List<Song> filteredSongs = new ArrayList<>();
    private AppDatabaseHelper dbHelper;
    private SessionManager session;
    private SongStatsHelper songStatsHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        searchHistoryManager = new SearchHistoryManager(this);
        playerManager = MusicPlayerManager.getInstance(this);
        playerManager.addListener(this);

        dbHelper = new com.example.musicapp.AppDatabaseHelper(this);
        session = new com.example.musicapp.SessionManager(this);
        songStatsHelper = new com.example.musicapp.utils.SongStatsHelper(this);

        setupRecentList();
        setupResultList();
        setupSearchInput();
        setupMiniPlayer();

        loadSongs();
        renderGenreButtons(allSongs);
        showRecentSearches();

        filteredSongs.clear();
        filteredSongs.addAll(allSongs);
        songAdapter.updateSongs(new ArrayList<>(filteredSongs));
        binding.textSearchResultInfo.setText(allSongs.size() + " bài hát");
        songAdapter.updatePlayCounts(songStatsHelper.getTopTrendingSongs(1000));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_search);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            if (id == R.id.nav_search) {
                return true;
            }

            if (id == R.id.nav_library) {
                startActivity(new Intent(this, PlaylistActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }

    private void setupRecentList() {
        binding.recyclerRecentSearches.setLayoutManager(new LinearLayoutManager(this));
        recentSearchAdapter = new RecentSearchAdapter(new RecentSearchAdapter.OnRecentClickListener() {
            @Override
            public void onRecentClick(String query) {
                binding.editTextSearchPage.setText(query);
                binding.editTextSearchPage.setSelection(query.length());
                performSearch(query, false);
            }

            @Override
            public void onRemoveClick(String query) {
                searchHistoryManager.removeQuery(query);
                showRecentSearches();
            }
        });
        binding.recyclerRecentSearches.setAdapter(recentSearchAdapter);

        binding.textClearAll.setOnClickListener(v -> {
            searchHistoryManager.clearHistory();
            showRecentSearches();
        });
    }

    private void setupResultList() {
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(filteredSongs, this);
        binding.recyclerSearchResults.setAdapter(songAdapter);
        binding.recyclerSearchResults.setHasFixedSize(true);
    }

    private void setupSearchInput() {
        binding.editTextSearchPage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s == null ? "" : s.toString().trim();
                if (keyword.isEmpty()) {
                    filteredSongs.clear();
                    songAdapter.updateSongs(new ArrayList<>(filteredSongs));
                    binding.textSearchResultInfo.setText("Nhập từ khóa để tìm bài hát");
                    showRecentSearches();
                } else {
                    performSearch(keyword, false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        binding.editTextSearchPage.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.editTextSearchPage.getText().toString().trim();
                performSearch(query, true);
                return true;
            }
            return false;
        });
    }

    private void loadSongs() {
        allSongs.clear();
        allSongs.addAll(getSongs());
    }

    private void performSearch(String query, boolean saveHistory) {
        if (query == null || query.trim().isEmpty()) {
            showRecentSearches();
            return;
        }

        if (saveHistory) {
            searchHistoryManager.saveQuery(query);
        }

        filteredSongs.clear();
        String keyword = query.trim().toLowerCase(Locale.getDefault());

        for (Song song : allSongs) {
            if (matchesSong(song, keyword)) {
                filteredSongs.add(song);
            }
        }

        songAdapter.updateSongs(new ArrayList<>(filteredSongs));
        songAdapter.updatePlayCounts(songStatsHelper.getTopTrendingSongs(1000));


        binding.layoutRecentHeader.setVisibility(android.view.View.GONE);
        binding.recyclerRecentSearches.setVisibility(android.view.View.GONE);

        binding.textSearchResultInfo.setText(
                filteredSongs.isEmpty()
                        ? "Không tìm thấy kết quả"
                        : filteredSongs.size() + " kết quả"
        );
    }

    private boolean matchesSong(Song song, String keyword) {
        String q = normalize(keyword);

        return normalize(song.title).contains(q)
                || normalize(song.artist).contains(q)
                || normalize(song.genre).contains(q)
                || normalize(song.lyricText).contains(q);
    }

    private String normalize(String text) {
        if (text == null) return "";
        String noAccent = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.toLowerCase().replaceAll("[^a-z0-9 ]", "");
    }

    private void showRecentSearches() {
        List<String> recents = searchHistoryManager.getRecentSearches();

        boolean hasRecent = !recents.isEmpty();
        binding.layoutRecentHeader.setVisibility(hasRecent ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.recyclerRecentSearches.setVisibility(hasRecent ? android.view.View.VISIBLE : android.view.View.GONE);
        recentSearchAdapter.submitList(recents);
    }

    private List<Song> getSongs() {
        List<Song> songs = new ArrayList<>();

        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                MediaStore.Audio.Media.DATA + " LIKE ?";

        String[] selectionArgs = new String[]{
                "/storage/emulated/0/Music/%"
        };

        try (Cursor cursor = getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    long albumId = cursor.getLong(albumIdColumn);
                    long duration = cursor.getLong(durationColumn);
                    String realPath = cursor.getString(dataColumn);

                    if (duration < 10000) continue;
                    if (realPath == null || realPath.trim().isEmpty()) continue;

                    java.io.File file = new java.io.File(realPath);
                    if (!file.exists()) continue;

                    String genre = extractGenreFromPath(realPath);

                    Song song = new Song(
                            id,
                            title != null ? title : "Unknown title",
                            artist != null ? artist : "Unknown artist",
                            realPath,
                            albumId,
                            genre
                    );

                    song.lyricText = LyricSearchUtils.loadLyricFromAssets(this, song.title);
                    songs.add(song);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return songs;
    }

    private String extractGenreFromPath(String path) {
        if (path == null) return "Khác";

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

            if (genre != null && !genre.trim().isEmpty()) {
                return genre.trim();
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }

        return "Khác";
    }

    private String extractGenreFromUri(Uri contentUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, contentUri);
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            return genre == null || genre.trim().isEmpty() ? "Khác" : genre.trim();
        } catch (Exception e) {
            return "Khác";
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) { }
        }
    }

    private void renderGenreButtons(List<Song> songs) {
        binding.layoutGenresSearch.removeAllViews();

        Set<String> genres = new LinkedHashSet<>();
        genres.add("Tất cả");

        for (Song song : songs) {
            if (song.genre != null && !song.genre.trim().isEmpty()) {
                genres.add(song.genre.trim());
            }
        }

        for (String genre : genres) {
            MaterialButton button = createGenreButton(genre);
            binding.layoutGenresSearch.addView(button);
        }
    }

    private MaterialButton createGenreButton(String genre) {
        MaterialButton button = new MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dpToPx(12), 0);
        button.setLayoutParams(params);

        button.setText(genre);
        button.setAllCaps(false);
        button.setCornerRadius(dpToPx(20));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(dpToPx(40));
        button.setMinimumHeight(dpToPx(40));
        button.setPadding(dpToPx(18), dpToPx(10), dpToPx(18), dpToPx(10));

        updateGenreButtonStyle(button, genre.equalsIgnoreCase(selectedGenre));

        button.setOnClickListener(v -> {
            selectedGenre = genre;
            refreshGenreButtonStates();
            applySearchFilter();
        });

        return button;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void refreshGenreButtonStates() {
        int count = binding.layoutGenresSearch.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = binding.layoutGenresSearch.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                boolean selected = button.getText().toString().equalsIgnoreCase(selectedGenre);
                updateGenreButtonStyle(button, selected);
            }
        }
    }

    private void updateGenreButtonStyle(MaterialButton button, boolean selected) {
        int bgColor = selected
                ? android.graphics.Color.parseColor("#6C4DFF")
                : android.graphics.Color.parseColor("#1E1E2E");

        button.setBackgroundColor(bgColor);
        button.setTextColor(android.graphics.Color.WHITE);
        button.setStrokeWidth(0);
    }

    private void applySearchFilter() {
        filteredSongs.clear();

        String keyword = binding.editTextSearchPage.getText() == null
                ? ""
                : binding.editTextSearchPage.getText().toString().trim().toLowerCase(Locale.getDefault());

        boolean allSelected = "Tất cả".equalsIgnoreCase(selectedGenre);

        for (Song song : allSongs) {
            boolean matchGenre = allSelected
                    || (song.genre != null && song.genre.equalsIgnoreCase(selectedGenre));

            boolean matchKeyword =
                    keyword.isEmpty()
                            || (song.title != null && song.title.toLowerCase(Locale.getDefault()).contains(keyword))
                            || (song.artist != null && song.artist.toLowerCase(Locale.getDefault()).contains(keyword));

            if (matchGenre && matchKeyword) {
                filteredSongs.add(song);
            }
        }

        songAdapter.updateSongs(new ArrayList<>(filteredSongs));
        songAdapter.updatePlayCounts(songStatsHelper.getTopTrendingSongs(1000));
        binding.textSearchResultInfo.setText(
                filteredSongs.isEmpty()
                        ? "Không có bài hát phù hợp"
                        : filteredSongs.size() + " bài hát"
        );
    }


    private void setupMiniPlayer() {
        binding.miniPlayer.setOnClickListener(v -> {
            if (!playerManager.hasPlaylist()) return;
            startActivity(new Intent(this, PlayerActivity.class));
        });

        binding.btnMiniPlay.setOnClickListener(v -> {
            binding.btnMiniPlay.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(90)
                    .withEndAction(() -> binding.btnMiniPlay.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .start())
                    .start();

            playerManager.togglePlayPause();
            syncMiniPlayer();
        });

        binding.btnPrev.setOnClickListener(v -> {
            playerManager.playPrevious();
            binding.miniAlbumArt.animate().rotationBy(360f).setDuration(350).start();
            syncMiniPlayer();
            saveCurrentSongToHistory();
        });

        binding.btnNext.setOnClickListener(v -> {
            playerManager.playNext();
            binding.miniAlbumArt.animate().rotationBy(360f).setDuration(350).start();
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

        if (session != null && session.isLoggedIn()) {
            dbHelper.savePlayHistory(
                    session.getUserId(),
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
        syncMiniPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerManager != null) {
            playerManager.removeListener(this);
        }
    }

    @Override
    public void onPlaybackStateChanged() {
        runOnUiThread(this::syncMiniPlayer);
    }
    @Override
    public void onItemClick(int position) {
        if (position < 0 || position >= filteredSongs.size()) return;

        String query = binding.editTextSearchPage.getText().toString().trim();
        if (!query.isEmpty()) {
            searchHistoryManager.saveQuery(query);
        }

        Song selectedSong = filteredSongs.get(position);

        if (selectedSong.data == null || selectedSong.data.trim().isEmpty()) return;

        java.io.File audioFile = new java.io.File(selectedSong.data);
        if (!audioFile.exists()) return;

        if (session != null && session.isLoggedIn()) {
            dbHelper.savePlayHistory(
                    session.getUserId(),
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

        playerManager.setPlaylist(new ArrayList<>(filteredSongs), position);

        songAdapter.updatePlayCounts(songStatsHelper.getTopTrendingSongs(1000));

        startActivity(new Intent(this, PlayerActivity.class));
    }
}