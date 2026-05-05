package com.example.musicapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Song implements Parcelable {
    public long id;
    public String title;
    public String artist;
    public String data;
    public long albumId;
    public String genre;

    public String lyricText;

    public Song(long id, String title, String artist, String data, long albumId, String genre) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.data = data;
        this.albumId = albumId;
        this.genre = genre;
        this.lyricText = "";
    }

    protected Song(Parcel in) {
        id = in.readLong();
        title = in.readString();
        artist = in.readString();
        data = in.readString();
        albumId = in.readLong();
        genre = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(data);
        dest.writeLong(albumId);
        dest.writeString(genre);
    }
}