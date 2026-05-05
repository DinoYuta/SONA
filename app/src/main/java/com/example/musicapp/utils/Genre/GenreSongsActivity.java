package com.example.musicapp.utils.Genre;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.AppDatabaseHelper;
import com.example.musicapp.R;
import com.example.musicapp.SessionManager;
import com.example.musicapp.model.Song;
import com.example.musicapp.utils.Music.MusicPlayerManager;
import com.example.musicapp.utils.Player.PlayerActivity;
import com.example.musicapp.utils.SongAdapter;
import com.example.musicapp.utils.SongStatsHelper;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenreSongsActivity extends AppCompatActivity implements SongAdapter.OnItemClickListerner {

    public static final String EXTRA_GENRE = "extra_genre";

    private TextView textGenreTitle;
    private TextView textGenreInfo;
    private RecyclerView recyclerViewGenreSongs;
    private SongAdapter adapter;
    private final List<Song> genreSongs = new ArrayList<>();
    private MusicPlayerManager playerManager;
    private SessionManager sessionManager;
    private AppDatabaseHelper dbHelper;
    private SongStatsHelper songStatsHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_songs);

        sessionManager = new SessionManager(this);
        dbHelper = new AppDatabaseHelper(this);
        songStatsHelper = new SongStatsHelper(this);

        textGenreTitle = findViewById(R.id.textGenreTitle);
        textGenreInfo = findViewById(R.id.textGenreInfo);
        recyclerViewGenreSongs = findViewById(R.id.recyclerViewGenreSongs);

        recyclerViewGenreSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(genreSongs, this);
        recyclerViewGenreSongs.setAdapter(adapter);

        playerManager = MusicPlayerManager.getInstance(this);

        String genre = getIntent().getStringExtra(EXTRA_GENRE);
        if (genre == null || genre.trim().isEmpty()) {
            genre = "Khác";
        }

        textGenreTitle.setText(genre);
        loadSongsByGenre(genre);
    }

    private void loadSongsByGenre(String targetGenre) {
        genreSongs.clear();
        genreSongs.addAll(getSongsByGenre(targetGenre));

        adapter.updateSongs(new ArrayList<>(genreSongs));
        textGenreInfo.setText(genreSongs.size() + " bài hát");
    }

    private List<Song> getSongsByGenre(String targetGenre) {
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

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = getContentResolver().query(
                collection,
                projection,
                selection,
                null,
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

                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                    );

                    String genre = extractGenreFromUri(contentUri);

                    if (!genre.equalsIgnoreCase(targetGenre)) continue;

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

    private String extractGenreFromUri(Uri contentUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, contentUri);
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            if (genre == null || genre.trim().isEmpty()) {
                return "Khác";
            }
            return genre.trim();
        } catch (Exception e) {
            return "Khác";
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onItemClick(int position) {
        Song selectedSong = genreSongs.get(position);

        if (position < 0 || position >= genreSongs.size()) return;

        if (sessionManager != null && sessionManager.isLoggedIn()) {
            dbHelper.savePlayHistory(
                    sessionManager.getUserId(),
                    selectedSong.title,
                    selectedSong.artist,
                    selectedSong.data
            );
        }

        songStatsHelper.incrementPlayCount(
                selectedSong.title,
                selectedSong.artist,
                selectedSong.data
        );

        playerManager.setPlaylist(new ArrayList<>(genreSongs), position);
        startActivity(new Intent(this, PlayerActivity.class));
    }
}