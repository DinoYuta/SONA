package com.example.musicapp.utils.Search;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SearchHistoryManager {

    private static final String PREF_NAME = "search_history_pref";
    private static final String KEY_HISTORY = "recent_searches";
    private static final int MAX_HISTORY = 5;

    private final SharedPreferences prefs;

    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveQuery(String query) {
        if (query == null) return;
        query = query.trim();
        if (query.isEmpty()) return;

        List<String> current = getRecentSearches();
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(query);

        for (String item : current) {
            if (!item.equalsIgnoreCase(query)) {
                ordered.add(item);
            }
        }

        List<String> result = new ArrayList<>(ordered);
        if (result.size() > MAX_HISTORY) {
            result = result.subList(0, MAX_HISTORY);
        }

        saveList(result);
    }

    public List<String> getRecentSearches() {
        List<String> result = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(arr.getString(i));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public void removeQuery(String query) {
        List<String> current = getRecentSearches();
        List<String> updated = new ArrayList<>();

        for (String item : current) {
            if (!item.equalsIgnoreCase(query)) {
                updated.add(item);
            }
        }

        saveList(updated);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    private void saveList(List<String> list) {
        JSONArray arr = new JSONArray();
        for (String item : list) {
            arr.put(item);
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
    }
}