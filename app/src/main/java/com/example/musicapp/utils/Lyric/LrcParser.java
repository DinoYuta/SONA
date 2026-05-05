package com.example.musicapp.utils.Lyric;

import com.example.musicapp.model.LyricLine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LrcParser {

    public static List<LyricLine> parse(InputStream inputStream) {
        List<LyricLine> lyrics = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            long fakeTime = 0L;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("[")) {
                    int closeIndex = line.indexOf("]");
                    if (closeIndex != -1) {
                        String timePart = line.substring(1, closeIndex).trim();
                        String textPart = line.substring(closeIndex + 1).trim();

                        long time = parseTimeToMillis(timePart);
                        if (time >= 0) {
                            lyrics.add(new LyricLine(time, textPart));
                            continue;
                        }
                    }
                }

                lyrics.add(new LyricLine(fakeTime, line));
                fakeTime += 4000;
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lyrics;
    }

    private static long parseTimeToMillis(String timeText) {
        try {
            String[] parts = timeText.split(":");
            if (parts.length != 2) return -1;

            int minutes = Integer.parseInt(parts[0]);
            float seconds = Float.parseFloat(parts[1]);

            return (long) ((minutes * 60 + seconds) * 1000);
        } catch (Exception e) {
            return -1;
        }
    }
}