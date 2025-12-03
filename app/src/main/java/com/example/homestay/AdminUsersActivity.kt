package com.example.homestay

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.data.api.ApiClient
import com.example.homestay.data.api.models.AdminUserData
import com.example.homestay.ui.admin.AdminUserAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AdminUsersActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUserAdapter
    private lateinit var progressBar: ProgressBar
    private var users = mutableListOf<AdminUserData>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)
        
        setupToolbar()
        setupViews()
        loadUsers()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Danh sách Users"
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.rv_users)
        progressBar = findViewById(R.id.progress_bar)
        
        adapter = AdminUserAdapter(
            users = users,
            onDeleteClick = { user -> showDeleteConfirmDialog(user) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminApiService.getUsers()
                if (response.isSuccessful && response.body()?.success == true) {
                    val usersList = response.body()?.users ?: emptyList()
                    users.clear()
                    users.addAll(usersList)
                    adapter.updateUsers(usersList)
                    android.util.Log.d("AdminUsers", "Loaded ${usersList.size} users")
                } else {
                    Toast.makeText(this@AdminUsersActivity, "Không thể tải danh sách users", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminUsers", "Error: ${e.message}", e)
                Toast.makeText(this@AdminUsersActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showDeleteConfirmDialog(user: AdminUserData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa user")
            .setMessage("Bạn có chắc chắn muốn xóa user \"${user.fullName}\" (${user.email})?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteUser(user.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun deleteUser(userId: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminApiService.deleteUser(userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.get("success") == true) {
                        Toast.makeText(this@AdminUsersActivity, "Xóa user thành công!", Toast.LENGTH_SHORT).show()
                        adapter.removeUser(userId)
                    } else {
                        val errorMsg = body?.get("error")?.toString() ?: "Xóa user thất bại"
                        Toast.makeText(this@AdminUsersActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Xóa user thất bại"
                    Toast.makeText(this@AdminUsersActivity, errorBody, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminUsersActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
