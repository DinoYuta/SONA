package com.example.musicapp.utils.Music;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.musicapp.model.Song;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class MusicPlayerManager {

    public interface PlaybackListener {
        void onPlaybackStateChanged();
    }

    private static MusicPlayerManager instance;

    private final Context appContext;
    private final List<PlaybackListener> listeners = new ArrayList<>();

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    private ArrayList<Song> currentPlaylist = new ArrayList<>();
    private int currentIndex = -1;

    private TelephonyManager telephonyManager;
    private CallCallback callCallback;
    private PhoneStateListener legacyPhoneStateListener;
    private TelephonyCallback modernCallback;

    private static final String PREF_NAME = "music_playback_state";
    private static final String KEY_PLAYLIST_PATHS = "playlist_paths";
    private static final String KEY_CURRENT_INDEX = "current_index";
    private static final String KEY_CURRENT_POSITION = "current_position";
    private static final String KEY_WAS_PLAYING = "was_playing";
    private static final String KEY_HAS_SAVED_STATE = "has_saved_state";
    private static final String KEY_NEEDS_RESTORE_AFTER_BOOT = "needs_restore_after_boot";

    private ArrayList<Song> pendingRestorePlaylist = null;
    private int pendingRestoreIndex = -1;
    private long pendingRestorePosition = 0L;
    private boolean pendingRestoreShouldPlay = false;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            syncCurrentIndexFromController();
            notifyPlaybackChanged();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            syncCurrentIndexFromController();
            notifyPlaybackChanged();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            syncCurrentIndexFromController();
            notifyPlaybackChanged();
        }
    };

    private MusicPlayerManager(Context context) {
        appContext = context.getApplicationContext();
        connectController();
    }

    public static synchronized MusicPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayerManager(context);
        }
        return instance;
    }

    private void connectController() {
        SessionToken sessionToken = new SessionToken(
                appContext,
                new ComponentName(appContext, MusicPlaybackService.class)
        );

        controllerFuture = new MediaController.Builder(appContext, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(playerListener);

                if (pendingRestorePlaylist != null && !pendingRestorePlaylist.isEmpty()) {
                    applyPlaylistToController(
                            pendingRestorePlaylist,
                            pendingRestoreIndex,
                            pendingRestorePosition,
                            pendingRestoreShouldPlay
                    );

                    pendingRestorePlaylist = null;
                    pendingRestoreIndex = -1;
                    pendingRestorePosition = 0L;
                    pendingRestoreShouldPlay = false;
                } else {
                    syncCurrentIndexFromController();
                    notifyPlaybackChanged();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(appContext));
    }

    private void syncCurrentIndexFromController() {
        if (controller == null) return;

        int index = controller.getCurrentMediaItemIndex();
        if (index >= 0 && index < currentPlaylist.size()) {
            currentIndex = index;
        }
    }

    private ArrayList<MediaItem> buildMediaItems(ArrayList<Song> songs) {
        ArrayList<MediaItem> mediaItems = new ArrayList<>();

        for (Song song : songs) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(song.data != null ? song.data : "")
                    .setUri(song.data)
                    .setMediaMetadata(metadata)
                    .build();

            mediaItems.add(mediaItem);
        }

        return mediaItems;
    }

    public Player getPlayer() {
        return controller;
    }

    public void addListener(PlaybackListener listener) {
        if (listener == null) return;
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlaybackChanged() {
        for (PlaybackListener listener : new ArrayList<>(listeners)) {
            listener.onPlaybackStateChanged();
        }
    }

    public void setPlaylist(ArrayList<Song> songs, int startIndex) {
        if (songs == null || songs.isEmpty()) return;
        if (controller == null) return;

        if (startIndex < 0 || startIndex >= songs.size()) {
            startIndex = 0;
        }

        currentPlaylist = new ArrayList<>(songs);
        currentIndex = startIndex;

        ArrayList<MediaItem> mediaItems = buildMediaItems(currentPlaylist);
        controller.setMediaItems(mediaItems, startIndex, 0);
        controller.prepare();
        controller.play();

        notifyPlaybackChanged();
        persistPlaybackState();
    }

    public void playSongAt(int index) {
        if (controller == null) return;
        if (currentPlaylist.isEmpty()) return;
        if (index < 0 || index >= currentPlaylist.size()) return;

        currentIndex = index;
        controller.seekTo(index, 0);
        controller.play();

        notifyPlaybackChanged();
        persistPlaybackState();
    }

    public void togglePlayPause() {
        if (controller == null) return;

        if (controller.isPlaying()) {
            controller.pause();
        } else {
            controller.play();
        }

        notifyPlaybackChanged();
        persistPlaybackState();
    }

    public void pause() {
        if (controller == null) return;

        controller.pause();
        notifyPlaybackChanged();
        persistPlaybackState();
    }

    public void playNext() {
        if (controller == null) return;
        if (currentPlaylist.isEmpty()) return;

        currentIndex = (currentIndex + 1) % currentPlaylist.size();
        controller.seekTo(currentIndex, 0);
        controller.play();

        notifyPlaybackChanged();
        persistPlaybackState();
    }

    public void playPrevious() {
        if (controller == null) return;
        if (currentPlaylist.isEmpty()) return;

        currentIndex = (currentIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
        controller.seekTo(currentIndex, 0);
        controller.play();

        notifyPlaybackChanged();
        persistPlaybackState();
    }

    public boolean hasPlaylist() {
        if (controller != null) {
            return controller.getMediaItemCount() > 0;
        }
        return !currentPlaylist.isEmpty();
    }

    public boolean isPlaying() {
        return controller != null && controller.isPlaying();
    }

    public int getCurrentIndex() {
        syncCurrentIndexFromController();
        return currentIndex;
    }

    @Nullable
    public Song getCurrentSong() {
        syncCurrentIndexFromController();

        if (currentPlaylist.isEmpty()) return null;
        if (currentIndex < 0 || currentIndex >= currentPlaylist.size()) return null;

        return currentPlaylist.get(currentIndex);
    }

    public ArrayList<Song> getCurrentPlaylist() {
        return new ArrayList<>(currentPlaylist);
    }

    public void stopAndClear() {
        if (controller != null) {
            controller.stop();
            controller.clearMediaItems();
        }

        currentPlaylist.clear();
        currentIndex = -1;

        clearSavedPlaybackState();
        notifyPlaybackChanged();
    }



    public void registerCallStateListener() {
        if (ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.READ_PHONE_STATE
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        telephonyManager =
                (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (modernCallback != null) return;

            modernCallback = new MyCallCallback();

            telephonyManager.registerTelephonyCallback(
                    appContext.getMainExecutor(),
                    modernCallback
            );
        } else {
            if (legacyPhoneStateListener != null) return;

            legacyPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    super.onCallStateChanged(state, phoneNumber);

                    if (state == TelephonyManager.CALL_STATE_RINGING
                            || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        pause();
                    }
                }
            };

            telephonyManager.listen(
                    legacyPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE
            );
        }
    }

    public void unregisterCallStateListener() {
        if (telephonyManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (modernCallback != null) {
                telephonyManager.unregisterTelephonyCallback(modernCallback);
                modernCallback = null;
            }
        } else {
            if (legacyPhoneStateListener != null) {
                telephonyManager.listen(legacyPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                legacyPhoneStateListener = null;
            }
        }
    }

    private Map<String, Song> buildSongMap(List<Song> songs) {
        Map<String, Song> map = new HashMap<>();
        if (songs == null) return map;

        for (Song song : songs) {
            if (song != null && song.data != null && !song.data.trim().isEmpty()) {
                map.put(song.data, song);
            }
        }
        return map;
    }

    private void applyPlaylistToController(ArrayList<Song> songs, int index, long positionMs, boolean shouldPlay) {
        if (controller == null || songs == null || songs.isEmpty()) return;

        if (index < 0 || index >= songs.size()) {
            index = 0;
        }

        currentPlaylist = new ArrayList<>(songs);
        currentIndex = index;

        ArrayList<MediaItem> mediaItems = buildMediaItems(currentPlaylist);
        controller.setMediaItems(mediaItems, currentIndex, Math.max(positionMs, 0));
        controller.prepare();

        if (shouldPlay) {
            controller.play();
        } else {
            controller.pause();
        }

        notifyPlaybackChanged();
    }

    public void persistPlaybackState() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        JSONArray array = new JSONArray();
        for (Song song : currentPlaylist) {
            if (song != null && song.data != null) {
                array.put(song.data);
            }
        }

        long currentPosition = 0L;
        boolean wasPlaying = false;

        if (controller != null) {
            currentPosition = Math.max(controller.getCurrentPosition(), 0L);
            wasPlaying = controller.isPlaying();
        }

        prefs.edit()
                .putString(KEY_PLAYLIST_PATHS, array.toString())
                .putInt(KEY_CURRENT_INDEX, currentIndex)
                .putLong(KEY_CURRENT_POSITION, currentPosition)
                .putBoolean(KEY_WAS_PLAYING, wasPlaying)
                .putBoolean(KEY_HAS_SAVED_STATE, true)
                .apply();
    }

    public void clearSavedPlaybackState() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_PLAYLIST_PATHS)
                .remove(KEY_CURRENT_INDEX)
                .remove(KEY_CURRENT_POSITION)
                .remove(KEY_WAS_PLAYING)
                .remove(KEY_HAS_SAVED_STATE)
                .remove(KEY_NEEDS_RESTORE_AFTER_BOOT)
                .apply();
    }

    public boolean restorePlaybackState(List<Song> availableSongs) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String rawPaths = prefs.getString(KEY_PLAYLIST_PATHS, null);
        if (rawPaths == null || rawPaths.trim().isEmpty()) {
            return false;
        }

        int savedIndex = prefs.getInt(KEY_CURRENT_INDEX, -1);
        long savedPosition = prefs.getLong(KEY_CURRENT_POSITION, 0L);
        boolean wasPlaying = prefs.getBoolean(KEY_WAS_PLAYING, false);

        Map<String, Song> songMap = buildSongMap(availableSongs);
        ArrayList<Song> restoredPlaylist = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(rawPaths);

            for (int i = 0; i < array.length(); i++) {
                String path = array.optString(i, null);
                if (path == null) continue;

                Song matchedSong = songMap.get(path);
                if (matchedSong != null) {
                    restoredPlaylist.add(matchedSong);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        if (restoredPlaylist.isEmpty()) {
            return false;
        }

        if (controller == null) {
            pendingRestorePlaylist = restoredPlaylist;
            pendingRestoreIndex = savedIndex;
            pendingRestorePosition = savedPosition;
            pendingRestoreShouldPlay = wasPlaying;
            return true;
        }

        applyPlaylistToController(restoredPlaylist, savedIndex, savedPosition, wasPlaying);
        return true;
    }

    public boolean needsRestoreAfterBoot() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NEEDS_RESTORE_AFTER_BOOT, false);
    }

    public void clearBootRestoreFlag() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NEEDS_RESTORE_AFTER_BOOT, false).apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static class CallCallback extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {

        private final MusicPlayerManager manager;

        public CallCallback(MusicPlayerManager manager) {
            this.manager = manager;
        }

        @Override
        public void onCallStateChanged(int state) {
            if (state == TelephonyManager.CALL_STATE_RINGING
                    || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                manager.stopAndClear();
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class MyCallCallback extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {

        @Override
        public void onCallStateChanged(int state) {
            if (state == TelephonyManager.CALL_STATE_RINGING
                    || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                pause();
            }
        }
    }
}