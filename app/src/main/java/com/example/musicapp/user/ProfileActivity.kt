package com.example.musicapp.user;

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musicapp.AppDatabaseHelper
import com.example.musicapp.SessionManager
import com.example.musicapp.R
import com.example.musicapp.utils.History.HistoryActivity
import com.example.musicapp.utils.Music.MusicPlayerManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var edtFullName: EditText
    private lateinit var tvEmail: TextView
    private lateinit var btnUpdateProfile: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnViewHistory: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        dbHelper = AppDatabaseHelper(this)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        edtFullName = findViewById(R.id.edtProfileFullName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnViewHistory = findViewById(R.id.btnViewHistory)
        btnLogout = findViewById(R.id.btnLogout)

        loadUser()

        btnUpdateProfile.setOnClickListener {
            val userId = sessionManager.getUserId()
            val fullName = edtFullName.text.toString().trim()

            if (fullName.isEmpty()) {
                edtFullName.error = "Vui lòng nhập họ tên"
                return@setOnClickListener
            }

            val success = dbHelper.updateProfile(userId, fullName, null)
            if (success) {
                val user = dbHelper.getUserById(userId)
                if (user != null) {
                    sessionManager.updateUserInfo(user.fullName, user.email)
                }
                Toast.makeText(this, "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
            }
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnLogout.setOnClickListener {
            MusicPlayerManager.getInstance(this).stopAndClear()
            sessionManager.logout()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUser() {
        val user = dbHelper.getUserById(sessionManager.getUserId())
        if (user != null) {
            edtFullName.setText(user.fullName)
            tvEmail.text = user.email
        }
    }
}