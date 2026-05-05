package com.example.musicapp.user

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musicapp.AppDatabaseHelper
import com.example.musicapp.R

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    private lateinit var edtEmailForgot: EditText
    private lateinit var edtNewPasswordForgot: EditText
    private lateinit var edtConfirmNewPasswordForgot: EditText
    private lateinit var btnSendReset: Button
    private lateinit var btnBackLoginForgot: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        dbHelper = AppDatabaseHelper(this)

        edtEmailForgot = findViewById(R.id.edtEmailForgot)
        edtNewPasswordForgot = findViewById(R.id.edtNewPasswordForgot)
        edtConfirmNewPasswordForgot = findViewById(R.id.edtConfirmNewPasswordForgot)
        btnSendReset = findViewById(R.id.btnSendReset)
        btnBackLoginForgot = findViewById(R.id.btnBackLoginForgot)

        btnSendReset.setOnClickListener {
            resetPasswordOffline()
        }

        btnBackLoginForgot.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun resetPasswordOffline() {
        val email = edtEmailForgot.text.toString().trim()
        val newPassword = edtNewPasswordForgot.text.toString().trim()
        val confirmPassword = edtConfirmNewPasswordForgot.text.toString().trim()

        when {
            email.isEmpty() -> {
                edtEmailForgot.error = "Vui lòng nhập email"
                edtEmailForgot.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                edtEmailForgot.error = "Email không hợp lệ"
                edtEmailForgot.requestFocus()
            }

            newPassword.length < 6 -> {
                edtNewPasswordForgot.error = "Mật khẩu tối thiểu 6 ký tự"
                edtNewPasswordForgot.requestFocus()
            }

            newPassword != confirmPassword -> {
                edtConfirmNewPasswordForgot.error = "Mật khẩu xác nhận không khớp"
                edtConfirmNewPasswordForgot.requestFocus()
            }

            !dbHelper.isEmailExists(email) -> {
                edtEmailForgot.error = "Email chưa được đăng ký"
                edtEmailForgot.requestFocus()
            }

            else -> {
                val success = dbHelper.resetPasswordByEmail(email, newPassword)
                if (success) {
                    Toast.makeText(this, "Đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Đặt lại mật khẩu thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}