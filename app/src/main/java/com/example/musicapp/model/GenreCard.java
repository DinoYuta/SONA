package com.example.musicapp.model;

public class GenreCard {
    private final String genreName;
    private final int songCount;

    public GenreCard(String genreName, int songCount) {
        this.genreName = genreName;
        this.songCount = songCount;
    }

    public String getGenreName() {
        return genreName;
    }

    public int getSongCount() {
        return songCount;
    }
}