package com.example.musicapp.model;

import java.util.List;

public class GenreMix {
    public String genre;
    public List<Song> topSongs;
    public double score;

    public GenreMix(String genre, List<Song> topSongs, double score) {
        this.genre = genre;
        this.topSongs = topSongs;
        this.score = score;
    }
}
