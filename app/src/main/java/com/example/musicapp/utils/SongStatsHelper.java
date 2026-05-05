package com.example.musicapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.musicapp.model.SongStat;

import java.util.ArrayList;
import java.util.List;

public class SongStatsHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "music_app.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_SONG_STATS = "song_stats";

    public SongStatsHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSongStatsTableIfNeeded(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        createSongStatsTableIfNeeded(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONG_STATS);
        createSongStatsTableIfNeeded(db);
    }

    private void createSongStatsTableIfNeeded(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_SONG_STATS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "song_title TEXT NOT NULL, " +
                "artist TEXT NOT NULL, " +
                "song_path TEXT NOT NULL UNIQUE, " +
                "total_play_count INTEGER NOT NULL DEFAULT 0, " +
                "last_played_at INTEGER NOT NULL" +
                ")";
        db.execSQL(sql);
    }

    public void incrementPlayCount(String songTitle, String artist, String songPath) {
        if (songPath == null || songPath.trim().isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id, total_play_count FROM " + TABLE_SONG_STATS + " WHERE song_path = ? LIMIT 1",
                new String[]{songPath}
        );

        long now = System.currentTimeMillis();

        try {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int currentCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_play_count"));

                ContentValues values = new ContentValues();
                values.put("song_title", songTitle != null ? songTitle : "Unknown title");
                values.put("artist", artist != null ? artist : "Unknown artist");
                values.put("total_play_count", currentCount + 1);
                values.put("last_played_at", now);

                db.update(TABLE_SONG_STATS, values, "id = ?", new String[]{String.valueOf(id)});
            } else {
                ContentValues values = new ContentValues();
                values.put("song_title", songTitle != null ? songTitle : "Unknown title");
                values.put("artist", artist != null ? artist : "Unknown artist");
                values.put("song_path", songPath);
                values.put("total_play_count", 1);
                values.put("last_played_at", now);

                db.insert(TABLE_SONG_STATS, null, values);
            }
        } finally {
            cursor.close();
        }
    }

    public List<SongStat> getTopTrendingSongs(int limit) {
        List<SongStat> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_SONG_STATS +
                        " ORDER BY total_play_count DESC, last_played_at DESC LIMIT ?",
                new String[]{String.valueOf(limit)}
        );

        try {
            while (cursor.moveToNext()) {
                list.add(new SongStat(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("song_title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                        cursor.getString(cursor.getColumnIndexOrThrow("song_path")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("total_play_count")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("last_played_at"))
                ));
            }
        } finally {
            cursor.close();
        }

        return list;
    }
}
