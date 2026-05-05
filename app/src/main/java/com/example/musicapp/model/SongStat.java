package com.example.musicapp.model;

public class SongStat {
    private final int id;
    private final String songTitle;
    private final String artist;
    private final String songPath;
    private final int totalPlayCount;
    private final long lastPlayedAt;

    public SongStat(int id, String songTitle, String artist, String songPath, int totalPlayCount, long lastPlayedAt) {
        this.id = id;
        this.songTitle = songTitle;
        this.artist = artist;
        this.songPath = songPath;
        this.totalPlayCount = totalPlayCount;
        this.lastPlayedAt = lastPlayedAt;
    }

    public int getId() {
        return id;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getArtist() {
        return artist;
    }

    public String getSongPath() {
        return songPath;
    }

    public int getTotalPlayCount() {
        return totalPlayCount;
    }

    public long getLastPlayedAt() {
        return lastPlayedAt;
    }
}