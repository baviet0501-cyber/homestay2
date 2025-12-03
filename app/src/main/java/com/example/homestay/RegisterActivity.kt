package com.example.homestay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.homestay.ui.viewmodel.AuthViewModel
import com.example.homestay.ui.viewmodel.AuthViewModelFactory
import com.example.homestay.utils.InputValidator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private val application by lazy { applicationContext as HomestayApplication }
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application.authRepository, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        observeRegisterResult()
    }

    private fun setupViews() {
        val etFullName = findViewById<TextInputEditText>(R.id.et_full_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPhone = findViewById<TextInputEditText>(R.id.et_phone)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnRegister = findViewById<MaterialButton>(R.id.btn_register)
        val tvLoginLink = findViewById<android.widget.TextView>(R.id.tv_login_link)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text?.toString()?.trim() ?: ""
            val email = etEmail.text?.toString()?.trim() ?: ""
            val phone = etPhone.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString() ?: ""
            val confirmPassword = etConfirmPassword.text?.toString() ?: ""

            // Tính năng 2: Validation mạnh mẽ
            // Validation cơ bản (validation chi tiết trong ViewModel)
            when {
                fullName.isEmpty() -> {
                    etFullName.error = "Vui lòng nhập họ và tên"
                    return@setOnClickListener
                }
                !InputValidator.validateFullName(fullName) -> {
                    etFullName.error = "Họ và tên không hợp lệ (2-50 ký tự)"
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    etEmail.error = "Vui lòng nhập email"
                    return@setOnClickListener
                }
                !InputValidator.validateEmail(email) -> {
                    etEmail.error = "Email không hợp lệ"
                    return@setOnClickListener
                }
                phone.isEmpty() -> {
                    etPhone.error = "Vui lòng nhập số điện thoại"
                    return@setOnClickListener
                }
                !InputValidator.validatePhoneNumber(phone) -> {
                    etPhone.error = "Số điện thoại không hợp lệ (10-11 số, bắt đầu bằng 0)"
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    etPassword.error = "Vui lòng nhập mật khẩu"
                    return@setOnClickListener
                }
                password != confirmPassword -> {
                    etConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                    return@setOnClickListener
                }
            }
            
            // Kiểm tra độ mạnh mật khẩu - hiển thị cảnh báo
            val passwordError = InputValidator.getPasswordErrorMessage(password)
            if (passwordError != "Mật khẩu hợp lệ") {
                etPassword.error = passwordError
                // Vẫn cho phép submit, validation chi tiết trong ViewModel
            }

            viewModel.register(fullName, email, phone, password)
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun observeRegisterResult() {
        lifecycleScope.launch {
            viewModel.registerResult.collect { result ->
                result?.let {
                    if (it.success) {
                        Toast.makeText(this@RegisterActivity, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, it.message ?: "Đăng ký thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

