package com.example.musicapp;

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("music_app_session", Context.MODE_PRIVATE)

    fun saveLogin(user: AppUser) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putInt("user_id", user.id)
            .putString("user_name", user.fullName)
            .putString("user_email", user.email)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getUserId(): Int {
        return prefs.getInt("user_id", -1)
    }

    fun getUserName(): String {
        return prefs.getString("user_name", "") ?: ""
    }

    fun getUserEmail(): String {
        return prefs.getString("user_email", "") ?: ""
    }

    fun updateUserInfo(fullName: String, email: String) {
        prefs.edit()
            .putString("user_name", fullName)
            .putString("user_email", email)
            .apply()
    }
}