package com.example.musicapp.model;

public class ScoredSong {
    public Song song;
    public int score;

    public ScoredSong(Song song, int score) {
        this.song = song;
        this.score = score;
    }
}
