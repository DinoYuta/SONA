package com.example.musicapp.utils.Music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    private static final String PREF_NAME = "music_playback_state";
    private static final String KEY_HAS_SAVED_STATE = "has_saved_state";
    private static final String KEY_NEEDS_RESTORE_AFTER_BOOT = "needs_restore_after_boot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean hasSavedState = prefs.getBoolean(KEY_HAS_SAVED_STATE, false);

        if (hasSavedState) {
            prefs.edit()
                    .putBoolean(KEY_NEEDS_RESTORE_AFTER_BOOT, true)
                    .apply();
        }
    }
}