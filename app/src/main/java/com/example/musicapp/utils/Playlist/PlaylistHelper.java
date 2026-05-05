package com.example.musicapp.utils.Playlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.musicapp.model.Song;

import java.util.ArrayList;
import java.util.List;

public class PlaylistHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "playlist.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_PLAYLIST = "playlist_songs";

    public PlaylistHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "artist TEXT, " +
                "song_path TEXT NOT NULL, " +
                "album_id INTEGER DEFAULT 0, " +
                "genre TEXT, " +
                "added_at INTEGER NOT NULL, " +
                "UNIQUE(user_id, song_path) ON CONFLICT IGNORE" +
                ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST);
        onCreate(db);
    }

    public boolean isSongInPlaylist(int userId, String songPath) {
        if (songPath == null || songPath.trim().isEmpty()) return false;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_PLAYLIST + " WHERE user_id = ? AND song_path = ? LIMIT 1",
                new String[]{String.valueOf(userId), songPath}
        );

        boolean exists = false;
        try {
            exists = cursor.moveToFirst();
        } finally {
            cursor.close();
        }
        return exists;
    }

    public boolean addSongToPlaylist(int userId, Song song) {
        if (song == null || song.data == null || song.data.trim().isEmpty()) return false;

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("title", song.title != null ? song.title : "Unknown title");
        values.put("artist", song.artist != null ? song.artist : "Unknown artist");
        values.put("song_path", song.data);
        values.put("album_id", song.albumId);
        values.put("genre", song.genre != null ? song.genre : "Khác");
        values.put("added_at", System.currentTimeMillis());

        long result = db.insert(TABLE_PLAYLIST, null, values);
        return result != -1;
    }

    public List<Song> getPlaylistSongs(int userId) {
        List<Song> songs = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_PLAYLIST + " WHERE user_id = ? ORDER BY added_at DESC",
                new String[]{String.valueOf(userId)}
        );

        try {
            while (cursor.moveToNext()) {
                songs.add(new Song(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                        cursor.getString(cursor.getColumnIndexOrThrow("song_path")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("album_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("genre"))
                ));
            }
        } finally {
            cursor.close();
        }

        return songs;
    }

    public int getPlaylistCount(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_PLAYLIST + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }

        return 0;
    }
}