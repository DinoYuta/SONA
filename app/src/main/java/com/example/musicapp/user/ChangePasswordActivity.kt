package com.example.musicapp.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musicapp.AppDatabaseHelper
import com.example.musicapp.R
import com.example.musicapp.SessionManager

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        dbHelper = AppDatabaseHelper(this)
        sessionManager = SessionManager(this)

        val edtNewPassword = findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirmNewPassword = findViewById<EditText>(R.id.edtConfirmNewPassword)
        val btnSavePassword = findViewById<Button>(R.id.btnSavePassword)

        btnSavePassword.setOnClickListener {
            val newPassword = edtNewPassword.text.toString().trim()
            val confirmPassword = edtConfirmNewPassword.text.toString().trim()

            when {
                newPassword.length < 6 -> {
                    edtNewPassword.error = "Mật khẩu tối thiểu 6 ký tự"
                }
                newPassword != confirmPassword -> {
                    edtConfirmNewPassword.error = "Mật khẩu xác nhận không khớp"
                }
                else -> {
                    val success = dbHelper.updatePassword(sessionManager.getUserId(), newPassword)
                    if (success) {
                        Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}