package com.example.musicapp.user;

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musicapp.user.LoginActivity
import com.example.musicapp.AppDatabaseHelper
import com.example.musicapp.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = AppDatabaseHelper(this)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnCreateAccount = findViewById<Button>(R.id.btnCreateAccount)
        val btnBackLogin = findViewById<Button>(R.id.btnBackLogin)

        btnCreateAccount.setOnClickListener {
            val fullName = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            when {
                fullName.isEmpty() -> edtName.error = "Vui lòng nhập họ tên"
                email.isEmpty() -> edtEmail.error = "Vui lòng nhập email"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> edtEmail.error = "Email không hợp lệ"
                password.length < 6 -> edtPassword.error = "Mật khẩu tối thiểu 6 ký tự"
                password != confirmPassword -> edtConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                else -> {
                    val success = dbHelper.registerUser(fullName, email, password)
                    if (success) {
                        Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Email đã tồn tại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnBackLogin.setOnClickListener {
            finish()
        }
    }
}