package com.example.musicapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicapp.databinding.ActivityMainBinding;
import com.example.musicapp.model.GenreCard;
import com.example.musicapp.model.Song;
import com.example.musicapp.model.SongStat;
import com.example.musicapp.user.LoginActivity;
import com.example.musicapp.user.ProfileActivity;
import com.example.musicapp.utils.Genre.GenreCardAdapter;
import com.example.musicapp.utils.Genre.GenreSongsActivity;
import com.example.musicapp.utils.Music.MusicPlayerManager;
import com.example.musicapp.utils.Player.PlayerActivity;
import com.example.musicapp.utils.Playlist.PlaylistActivity;
import com.example.musicapp.utils.Search.SearchActivity;
import com.example.musicapp.utils.SongAdapter;
import com.example.musicapp.utils.SongStatsHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements SongAdapter.OnItemClickListerner, MusicPlayerManager.PlaybackListener {

    // =========================================================
    // 1) VIEW + MANAGER + DATA
    // =========================================================
    private ActivityMainBinding binding;

    private SongAdapter adapter;
    private GenreCardAdapter genreCardAdapter;
    private MusicPlayerManager playerManager;

    private SessionManager session;
    private AppDatabaseHelper dbHelper;
    private SongStatsHelper songStatsHelper;

    private final List<Song> allSongs = new ArrayList<>();
    private final List<Song> filteredSongs = new ArrayList<>();
    private final List<GenreCard> forYouGenreCards = new ArrayList<>();

    private int currentPosition = -1;
    private ObjectAnimator miniDiscAnimator;

    // =========================================================
    // 2) PERMISSION LAUNCHER - đọc nhạc trong máy
    // =========================================================
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    scanAllMusicAndLoad();
                } else {
                    Toast.makeText(this, "Permission denied to read storage", Toast.LENGTH_SHORT).show();
                }
            });

    // =========================================================
    // 3) ACTIVITY LIFECYCLE
    // =========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initCore();

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initView();
        initPlayer();
        initSongList();
        initForYouGenres();

        setupWindowInsets();
        setupProfileButton();
        setupMiniPlayer();
        setupBottomNav();

        checkPermissionAndLoadSongs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_home);

        showForYouGenreCards();
        syncMiniPlayer();
        refreshTrendingRealtime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerManager != null) {
            playerManager.unregisterCallStateListener();
            playerManager.removeListener(this);
        }

        stopMiniDiscRotation();
    }

    @Override
    public void onPlaybackStateChanged() {
        runOnUiThread(this::syncMiniPlayer);
    }

    // =========================================================
    // 4) INIT
    // =========================================================
    private void initCore() {
        dbHelper = new AppDatabaseHelper(this);
        session = new SessionManager(this);
        songStatsHelper = new SongStatsHelper(this);
    }

    private void initView() {
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.searchOverlay.setVisibility(View.GONE);
    }

    private void initPlayer() {
        playerManager = MusicPlayerManager.getInstance(this);
        playerManager.addListener(this);
        requestPhoneStatePermissionIfNeeded();
    }

    private void initSongList() {
        binding.recyclerViewSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(filteredSongs, this);
        binding.recyclerViewSongs.setAdapter(adapter);
        binding.recyclerViewSongs.setHasFixedSize(true);
    }

    private void initForYouGenres() {
        binding.recyclerViewForYouGenres.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        genreCardAdapter = new GenreCardAdapter(genreCard -> {
            Intent intent = new Intent(MainActivity.this, GenreSongsActivity.class);
            intent.putExtra(GenreSongsActivity.EXTRA_GENRE, genreCard.getGenreName());
            startActivity(intent);
        });

        binding.recyclerViewForYouGenres.setAdapter(genreCardAdapter);
        binding.recyclerViewForYouGenres.setHasFixedSize(true);
    }

    // =========================================================
    // 5) UI SETUP
    // =========================================================
    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );

            android.view.ViewGroup.MarginLayoutParams params =
                    (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();

            params.bottomMargin = dpToPx(16) + bars.bottom;
            view.setLayoutParams(params);

            return insets;
        });

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(0, bars.top, 0, 0);
            return insets;
        });
    }

    private void setupProfileButton() {
        binding.btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            }

            if (id == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            if (id == R.id.nav_library) {
                startActivity(new Intent(this, PlaylistActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

    // =========================================================
    // 6) MINI PLAYER
    // =========================================================
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

    private void syncMiniPlayer() {
        Song currentSong = playerManager.getCurrentSong();
        updateMiniPlayer(currentSong);

        if (currentSong == null) return;

        binding.btnMiniPlay.setImageResource(
                playerManager.isPlaying()
                        ? R.drawable.ic_pause_24
                        : R.drawable.ic_play_arrow_24
        );

        currentPosition = playerManager.getCurrentIndex();

        if (playerManager.isPlaying()) {
            startMiniDiscRotation();
        } else {
            stopMiniDiscRotation();
        }
    }

    private void updateMiniPlayer(Song song) {
        if (song == null) {
            binding.miniPlayer.setVisibility(View.GONE);
            return;
        }

        binding.miniPlayer.setVisibility(View.VISIBLE);
        binding.miniTitle.setText(song.title != null ? song.title : "");
        binding.miniArtist.setText(song.artist != null ? song.artist : "");

        binding.miniPlayer.setAlpha(0f);
        binding.miniPlayer.setTranslationY(50f);
        binding.miniPlayer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .start();

        Bitmap embeddedArt = getEmbeddedAlbumArt(song.data);

        if (embeddedArt != null) {
            binding.miniAlbumArt.setImageBitmap(embeddedArt);
        } else if (song.albumId > 0) {
            Uri albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    song.albumId
            );

            Glide.with(this).clear(binding.miniAlbumArt);

            Glide.with(this)
                    .load(albumArtUri)
                    .placeholder(R.drawable.ic_music_note_24)
                    .error(R.drawable.ic_music_note_24)
                    .into(binding.miniAlbumArt);
        } else {
            binding.miniAlbumArt.setImageResource(R.drawable.ic_music_note_24);
        }
    }

    private void startMiniDiscRotation() {
        stopMiniDiscRotation();

        miniDiscAnimator = ObjectAnimator.ofFloat(
                binding.miniAlbumArt,
                "rotation",
                binding.miniAlbumArt.getRotation(),
                binding.miniAlbumArt.getRotation() + 360f
        );
        miniDiscAnimator.setDuration(10000);
        miniDiscAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        miniDiscAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        miniDiscAnimator.start();
    }

    private void stopMiniDiscRotation() {
        if (miniDiscAnimator != null) {
            miniDiscAnimator.cancel();
            miniDiscAnimator = null;
        }
    }

    // =========================================================
    // 7) CLICK BÀI HÁT
    // =========================================================
    @Override
    public void onItemClick(int position) {
        currentPosition = position;

        Song selectedSong = filteredSongs.get(position);

        if (selectedSong.data == null || selectedSong.data.trim().isEmpty()) {
            Toast.makeText(this, "File bài hát không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(selectedSong.data);
        if (!audioFile.exists()) {
            Toast.makeText(this, "Bài hát này đã bị xóa khỏi thiết bị", Toast.LENGTH_SHORT).show();

            filteredSongs.remove(position);
            adapter.updateSongs(new ArrayList<>(filteredSongs));
            binding.textResultInfo.setText(filteredSongs.size() + " bài hát");
            return;
        }

        if (session != null && session.isLoggedIn()) {
            dbHelper.savePlayHistory(
                    session.getUserId(),
                    selectedSong.title != null ? selectedSong.title : "Unknown title",
                    selectedSong.artist != null ? selectedSong.artist : "Unknown artist",
                    selectedSong.data != null ? selectedSong.data : ""
            );
        }

        songStatsHelper.incrementPlayCount(
                selectedSong.title,
                selectedSong.artist,
                selectedSong.data
        );

        refreshTrendingRealtime();

        playerManager.setPlaylist(new ArrayList<>(filteredSongs), position);
        syncMiniPlayer();
        startActivity(new Intent(this, PlayerActivity.class));
    }

    private void saveCurrentSongToHistory() {
        Song currentSong = playerManager.getCurrentSong();
        if (currentSong == null) return;
        if (currentSong.data == null || currentSong.data.trim().isEmpty()) return;

        File audioFile = new File(currentSong.data);
        if (!audioFile.exists()) return;

        if (session != null && session.isLoggedIn()) {
            dbHelper.savePlayHistory(
                    session.getUserId(),
                    currentSong.title != null ? currentSong.title : "Unknown title",
                    currentSong.artist != null ? currentSong.artist : "Unknown artist",
                    currentSong.data != null ? currentSong.data : ""
            );
        }

        songStatsHelper.incrementPlayCount(
                currentSong.title,
                currentSong.artist,
                currentSong.data
        );

        refreshTrendingRealtime();
    }

    // =========================================================
    // 8) TRENDING + FOR YOU
    // =========================================================
    private void showTrendingTop10Songs() {
        refreshTrendingRealtime();
    }

    private void refreshTrendingRealtime() {
        List<Song> trendingSongs = getTrendingTop10Songs();

        filteredSongs.clear();
        filteredSongs.addAll(trendingSongs);

        adapter.updateSongs(new ArrayList<>(filteredSongs));

        List<SongStat> stats = songStatsHelper.getTopTrendingSongs(50);
        adapter.updatePlayCounts(stats);

        binding.textResultInfo.setText(
                trendingSongs.isEmpty()
                        ? "Không có bài hát trending"
                        : "Top " + trendingSongs.size() + " bài trending"
        );
    }

    private List<Song> getTrendingTop10Songs() {
        List<Song> trendingSongs = new ArrayList<>();
        List<SongStat> topStats = songStatsHelper.getTopTrendingSongs(10);
        Set<String> addedPaths = new HashSet<>();

        for (SongStat stat : topStats) {
            for (Song song : allSongs) {
                if (song.data != null
                        && stat.getSongPath() != null
                        && song.data.equals(stat.getSongPath())
                        && !addedPaths.contains(song.data)) {
                    trendingSongs.add(song);
                    addedPaths.add(song.data);
                    break;
                }
            }
        }

        if (trendingSongs.isEmpty()) {
            for (int i = 0; i < Math.min(10, allSongs.size()); i++) {
                trendingSongs.add(allSongs.get(i));
            }
        }

        return trendingSongs;
    }

    private void showForYouGenreCards() {
        List<GenreCard> genreCards = getForYouGenreCards();

        forYouGenreCards.clear();
        forYouGenreCards.addAll(genreCards);

        if (genreCardAdapter != null) {
            genreCardAdapter.submitList(new ArrayList<>(forYouGenreCards));
        }
    }

    private List<GenreCard> getForYouGenreCards() {
        List<GenreCard> result = new ArrayList<>();

        if (session == null || !session.isLoggedIn()) {
            return result;
        }

        List<PlayHistory> historyList = dbHelper.getHistoryByUser(session.getUserId());
        if (historyList == null || historyList.isEmpty()) {
            return result;
        }

        Map<String, Integer> genreCountMap = new HashMap<>();

        for (PlayHistory history : historyList) {
            String songPath = history.getSongPath();
            if (songPath == null || songPath.trim().isEmpty()) continue;

            for (Song song : allSongs) {
                if (song.data != null && song.data.equals(songPath)) {
                    String genre = (song.genre == null || song.genre.trim().isEmpty())
                            ? "Khác"
                            : song.genre.trim();

                    genreCountMap.put(
                            genre,
                            genreCountMap.getOrDefault(genre, 0) + history.getPlayCount()
                    );
                    break;
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedGenres = new ArrayList<>(genreCountMap.entrySet());
        Collections.sort(sortedGenres, (o1, o2) -> Integer.compare(o2.getValue(), o1.getValue()));

        for (Map.Entry<String, Integer> entry : sortedGenres) {
            int songCount = countSongsByGenre(entry.getKey());
            result.add(new GenreCard(entry.getKey(), songCount));

            if (result.size() >= 5) {
                break;
            }
        }

        return result;
    }

    private int countSongsByGenre(String genreName) {
        int count = 0;
        for (Song song : allSongs) {
            if (song.genre != null && song.genre.equalsIgnoreCase(genreName)) {
                count++;
            }
        }
        return count;
    }

    // =========================================================
    // 9) LOAD SONG TỪ /storage/emulated/0/Music
    // =========================================================
    private void checkPermissionAndLoadSongs() {
        String permission;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            scanAllMusicAndLoad();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void scanAllMusicAndLoad() {
        File musicDir = new File("/storage/emulated/0/Music");
        List<String> paths = new ArrayList<>();
        collectAudioPaths(musicDir, paths);

        if (!paths.isEmpty()) {
            android.media.MediaScannerConnection.scanFile(
                    this,
                    paths.toArray(new String[0]),
                    null,
                    (path, uri) -> { }
            );
        }

        binding.recyclerViewSongs.postDelayed(this::loadSongsAsync, 300);
    }

    private void collectAudioPaths(File dir, List<String> out) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectAudioPaths(file, out);
            } else {
                String name = file.getName().toLowerCase(Locale.getDefault());
                if (name.endsWith(".mp3")
                        || name.endsWith(".wav")
                        || name.endsWith(".m4a")
                        || name.endsWith(".flac")) {
                    out.add(file.getAbsolutePath());
                }
            }
        }
    }

    private void loadSongsAsync() {
        new Thread(() -> {

            List<Song> songs = getSongs();

            runOnUiThread(() -> {

                allSongs.clear();
                allSongs.addAll(songs);

                boolean restored =
                        playerManager.restorePlaybackState(new ArrayList<>(allSongs));

                if (restored && playerManager.needsRestoreAfterBoot()) {
                    playerManager.clearBootRestoreFlag();
                }

                showForYouGenreCards();
                showTrendingTop10Songs();
                syncMiniPlayer();

                Toast.makeText(
                        this,
                        allSongs.isEmpty()
                                ? "Không đọc được bài hát"
                                : "Đã tìm thấy " + allSongs.size() + " bài hát",
                        Toast.LENGTH_SHORT
                ).show();
            });

        }).start();
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

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                + MediaStore.Audio.Media.DATA + " LIKE ?";

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

                    File file = new File(realPath);
                    if (!file.exists()) continue;

                    String genre = extractGenreFromPath(realPath);

                    songs.add(new Song(
                            id,
                            title != null ? title : "Unknown title",
                            artist != null ? artist : "Unknown artist",
                            realPath,
                            albumId,
                            genre
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return songs;
    }

    // =========================================================
    // 10) GENRE HELPER
    // =========================================================
    private String extractGenreFromPath(String songPath) {
        if (songPath == null || songPath.trim().isEmpty()) {
            return "Khác";
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(songPath);

            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

            if (genre != null) {
                genre = genre.trim();

                if (!genre.isEmpty()
                        && !genre.equalsIgnoreCase("unknown")
                        && !genre.equalsIgnoreCase("null")
                        && !genre.equalsIgnoreCase("(null)")) {
                    return normalizeGenre(genre);
                }
            }

            return inferGenreFromPath(songPath);
        } catch (Exception e) {
            return inferGenreFromPath(songPath);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private String normalizeGenre(String genre) {
        if (genre == null) return "Khác";

        String g = genre.trim().toLowerCase(Locale.getDefault());

        if (g.contains("nhạc xưa") || g.contains("nhac xua")) return "Nhạc xưa";
        if (g.contains("nhạc buồn") || g.contains("nhac buon")) return "Nhạc buồn";
        if (g.contains("ballad")) return "Ballad";
        if (g.contains("lofi")) return "Lofi";
        if (g.contains("chill")) return "Chill";
        if (g.contains("acoustic")) return "Acoustic";
        if (g.contains("edm")) return "EDM";
        if (g.contains("remix")) return "Remix";
        if (g.contains("rock")) return "Rock";
        if (g.contains("rap")) return "Rap";
        if (g.contains("hip hop") || g.contains("hiphop") || g.contains("hip-hop")) return "Hip Hop";
        if (g.contains("jazz")) return "Jazz";
        if (g.contains("pop")) return "Pop";

        return genre.trim();
    }

    private String inferGenreFromPath(String songPath) {
        String lower = songPath.toLowerCase(Locale.getDefault());

        if (lower.contains("nhạc xưa") || lower.contains("nhac xua")) return "Nhạc xưa";
        if (lower.contains("lofi")) return "Lofi";
        if (lower.contains("remix")) return "Remix";
        if (lower.contains("acoustic")) return "Acoustic";
        if (lower.contains("rock")) return "Rock";
        if (lower.contains("rap")) return "Rap";
        if (lower.contains("hiphop") || lower.contains("hip-hop")) return "Hip Hop";
        if (lower.contains("jazz")) return "Jazz";
        if (lower.contains("pop")) return "Pop";
        if (lower.contains("edm")) return "EDM";
        if (lower.contains("ballad")) return "Ballad";
        if (lower.contains("buồn") || lower.contains("nhac buon")) return "Nhạc buồn";
        if (lower.contains("chill")) return "Chill";

        return "Khác";
    }

    // =========================================================
    // 11) ART HELPER
    // =========================================================
    private Bitmap getEmbeddedAlbumArt(String songPath) {
        if (songPath == null || songPath.trim().isEmpty()) return null;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(songPath);
            byte[] artBytes = retriever.getEmbeddedPicture();

            if (artBytes != null && artBytes.length > 0) {
                return android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
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

    @Override
    protected void onPause() {
        super.onPause();
        if (playerManager != null) {
            playerManager.persistPlaybackState();
        }
    }

    // =========================================================
    // 12) PHONE CALL PERMISSION
    // =========================================================
    private void requestPhoneStatePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 2001);
            } else {
                playerManager.registerCallStateListener();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2001) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                playerManager.registerCallStateListener();
            }
        }
    }

    // =========================================================
    // 13) UTIL
    // =========================================================
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}