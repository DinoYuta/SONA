package com.example.musicapp.utils.Lyric;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;

public class LyricSearchUtils {

    public static String loadLyricFromAssets(Context context, String songTitle) {
        try {
            String fileName = normalize(songTitle) + ".lrc";

            String[] files = context.getAssets().list("");

            if (files == null) return "";

            for (String f : files) {
                String normalized = normalize(f.replace(".lrc", ""));

                if (normalized.equals(normalize(songTitle))) {
                    InputStream is = context.getAssets().open(f);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        line = removeTimeTags(line).trim();
                        if (!line.isEmpty()) {
                            sb.append(line).append(" ");
                        }
                    }

                    reader.close();
                    return sb.toString().toLowerCase();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String removeTimeTags(String line) {
        return line.replaceAll("\\[(\\d{1,2}:\\d{1,2}(\\.\\d{1,2})?)\\]", " ");
    }

    private static String normalize(String text) {
        if (text == null) return "";
        String noAccent = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}