package com.example.musicapp;

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AppUser(
    val id: Int = 0,
    val fullName: String,
    val email: String,
    val password: String,
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlayHistory(
    val id: Int = 0,
    val userId: Int,
    val songTitle: String,
    val artist: String,
    val songPath: String,
    val playedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)

class AppDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                avatar_uri TEXT,
                created_at INTEGER NOT NULL
            )
        """.trimIndent()

        val createHistoryTable = """
            CREATE TABLE $TABLE_HISTORY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                song_title TEXT NOT NULL,
                artist TEXT NOT NULL,
                song_path TEXT NOT NULL,
                played_at INTEGER NOT NULL,
                play_count INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(id)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createHistoryTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun registerUser(fullName: String, email: String, password: String): Boolean {
        if (isEmailExists(email)) return false

        val db = writableDatabase
        val values = ContentValues().apply {
            put("full_name", fullName)
            put("email", email.trim().lowercase())
            put("password", password)
            put("created_at", System.currentTimeMillis())
        }

        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    fun loginUser(email: String, password: String): AppUser? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_USERS
            WHERE email = ? AND password = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(email.trim().lowercase(), password)
        )

        var user: AppUser? = null
        if (cursor.moveToFirst()) {
            user = AppUser(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatar_uri")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
            )
        }
        cursor.close()
        return user
    }

    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM $TABLE_USERS WHERE email = ? LIMIT 1",
            arrayOf(email.trim().lowercase())
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getUserById(userId: Int): AppUser? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE id = ? LIMIT 1",
            arrayOf(userId.toString())
        )

        var user: AppUser? = null
        if (cursor.moveToFirst()) {
            user = AppUser(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                avatarUri = cursor.getString(cursor.getColumnIndexOrThrow("avatar_uri")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
            )
        }
        cursor.close()
        return user
    }

    fun updateProfile(userId: Int, fullName: String, avatarUri: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("full_name", fullName)
            put("avatar_uri", avatarUri)
        }

        val result = db.update(
            TABLE_USERS,
            values,
            "id = ?",
            arrayOf(userId.toString())
        )
        return result > 0
    }

    fun updatePassword(userId: Int, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("password", newPassword)
        }

        val result = db.update(
            TABLE_USERS,
            values,
            "id = ?",
            arrayOf(userId.toString())
        )
        return result > 0
    }

    fun savePlayHistory(userId: Int, songTitle: String, artist: String, songPath: String) {
        val db = writableDatabase

        val cursor = db.rawQuery(
            """
            SELECT id, play_count FROM $TABLE_HISTORY
            WHERE user_id = ? AND song_path = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(userId.toString(), songPath)
        )

        if (cursor.moveToFirst()) {
            val historyId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val currentCount = cursor.getInt(cursor.getColumnIndexOrThrow("play_count"))
            cursor.close()

            val values = ContentValues().apply {
                put("played_at", System.currentTimeMillis())
                put("play_count", currentCount + 1)
                put("song_title", songTitle)
                put("artist", artist)
            }

            db.update(TABLE_HISTORY, values, "id = ?", arrayOf(historyId.toString()))
        } else {
            cursor.close()
            val values = ContentValues().apply {
                put("user_id", userId)
                put("song_title", songTitle)
                put("artist", artist)
                put("song_path", songPath)
                put("played_at", System.currentTimeMillis())
                put("play_count", 1)
            }
            db.insert(TABLE_HISTORY, null, values)
        }
    }

    fun resetPasswordByEmail(email: String, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("password", newPassword)
        }

        val result = db.update(
            TABLE_USERS,
            values,
            "email = ?",
            arrayOf(email.trim().lowercase())
        )
        return result > 0
    }

    fun getHistoryByUser(userId: Int): MutableList<PlayHistory> {
        val list = mutableListOf<PlayHistory>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_HISTORY
            WHERE user_id = ?
            ORDER BY played_at DESC
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        while (cursor.moveToNext()) {
            list.add(
                PlayHistory(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                    songTitle = cursor.getString(cursor.getColumnIndexOrThrow("song_title")),
                    artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                    songPath = cursor.getString(cursor.getColumnIndexOrThrow("song_path")),
                    playedAt = cursor.getLong(cursor.getColumnIndexOrThrow("played_at")),
                    playCount = cursor.getInt(cursor.getColumnIndexOrThrow("play_count"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun clearHistory(userId: Int): Boolean {
        val db = writableDatabase
        val deleted = db.delete(TABLE_HISTORY, "user_id = ?", arrayOf(userId.toString()))
        return deleted >= 0
    }

    companion object {
        private const val DB_NAME = "music_app.db"
        private const val DB_VERSION = 1

        private const val TABLE_USERS = "users"
        private const val TABLE_HISTORY = "history"
    }
}