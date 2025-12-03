package com.example.homestay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.homestay.data.api.ApiClient
import com.example.homestay.data.api.models.AdminLoginRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AdminLoginActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)
        
        setupViews()
    }
    
    private fun setupViews() {
        val etUsername = findViewById<TextInputEditText>(R.id.et_username)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val btnCancel = findViewById<MaterialButton>(R.id.btn_cancel)
        
        btnLogin.setOnClickListener {
            val username = etUsername.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString() ?: ""
            
            if (username.isEmpty()) {
                etUsername.error = "Vui lòng nhập username"
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập password"
                return@setOnClickListener
            }
            
            // Disable button
            btnLogin.isEnabled = false
            btnLogin.text = "Đang đăng nhập..."
            
            adminLogin(username, password, btnLogin)
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun adminLogin(username: String, password: String, btnLogin: MaterialButton) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("AdminLogin", "Attempting login: username=$username")
                android.util.Log.d("AdminLogin", "API URL: ${com.example.homestay.data.api.ApiConfig.BASE_URL}admin/login")
                
                val response = ApiClient.adminApiService.adminLogin(
                    AdminLoginRequest(username, password)
                )
                
                android.util.Log.d("AdminLogin", "Response code: ${response.code()}")
                android.util.Log.d("AdminLogin", "Response body: ${response.body()}")
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    android.util.Log.d("AdminLogin", "Success: ${loginResponse?.success}")
                    
                    if (loginResponse?.success == true) {
                        val admin = loginResponse.admin
                        if (admin != null) {
                            android.util.Log.d("AdminLogin", "Admin logged in: ${admin.username}")
                            
                            // Lưu admin session
                            saveAdminSession(admin.id, admin.username, admin.fullName, admin.role)
                            
                            Toast.makeText(this@AdminLoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            
                            // Chuyển sang AdminDashboardActivity
                            val intent = Intent(this@AdminLoginActivity, AdminDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                            return@launch
                        }
                    } else {
                        android.util.Log.e("AdminLogin", "Login failed: ${loginResponse?.error}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AdminLogin", "API error: ${response.code()} - $errorBody")
                }
                
                // Show error
                btnLogin.isEnabled = true
                btnLogin.text = "Đăng nhập"
                val errorMsg = response.body()?.error ?: response.errorBody()?.string() ?: "Sai tài khoản hoặc mật khẩu"
                Toast.makeText(this@AdminLoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("AdminLogin", "Exception: ${e.message}", e)
                btnLogin.isEnabled = true
                btnLogin.text = "Đăng nhập"
                Toast.makeText(this@AdminLoginActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveAdminSession(id: String, username: String, fullName: String, role: String) {
        val prefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        prefs.edit().apply {
            putString("admin_id", id)
            putString("admin_username", username)
            putString("admin_fullname", fullName)
            putString("admin_role", role)
            putBoolean("is_admin_logged_in", true)
            apply()
        }
    }
}

