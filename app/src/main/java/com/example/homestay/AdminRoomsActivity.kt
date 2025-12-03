package com.example.homestay

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.data.api.ApiClient
import com.example.homestay.data.api.models.AdminRoomData
import com.example.homestay.data.api.models.RoomRequest
import com.example.homestay.ui.admin.AdminRoomAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AdminRoomsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminRoomAdapter
    private lateinit var fabAddRoom: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private var rooms = mutableListOf<AdminRoomData>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_rooms)
        
        setupToolbar()
        setupViews()
        loadRooms()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Quản lý Phòng"
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.rv_rooms)
        fabAddRoom = findViewById(R.id.fab_add_room)
        progressBar = findViewById(R.id.progress_bar)
        
        adapter = AdminRoomAdapter(
            rooms = rooms,
            onEditClick = { room -> showEditRoomDialog(room) },
            onDeleteClick = { room -> showDeleteConfirmDialog(room) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        fabAddRoom.setOnClickListener {
            showAddRoomDialog()
        }
    }
    
    private fun loadRooms() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminApiService.getRooms()
                if (response.isSuccessful && response.body()?.success == true) {
                    val roomsList = response.body()?.rooms ?: emptyList()
                    rooms.clear()
                    rooms.addAll(roomsList)
                    adapter.updateRooms(roomsList)
                    android.util.Log.d("AdminRooms", "Loaded ${roomsList.size} rooms")
                } else {
                    Toast.makeText(this@AdminRoomsActivity, "Không thể tải danh sách phòng", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminRooms", "Error: ${e.message}", e)
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showAddRoomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_room, null)
        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_room_name)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_room_description)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.et_room_price)
        val etCapacity = dialogView.findViewById<TextInputEditText>(R.id.et_room_capacity)
        val etMaxSlots = dialogView.findViewById<TextInputEditText>(R.id.et_room_max_slots)
        val etImageUrl = dialogView.findViewById<TextInputEditText>(R.id.et_room_image_url)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)
        
        tvTitle.text = "Thêm phòng mới"
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val description = etDescription.text?.toString()?.trim() ?: ""
            val priceStr = etPrice.text?.toString()?.trim() ?: ""
            val capacityStr = etCapacity.text?.toString()?.trim() ?: ""
            val maxSlotsStr = etMaxSlots.text?.toString()?.trim() ?: "1"
            val imageUrl = etImageUrl.text?.toString()?.trim() ?: ""
            
            if (name.isEmpty() || priceStr.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val price = priceStr.toDoubleOrNull() ?: 0.0
            val capacity = capacityStr.toIntOrNull() ?: 1
            val maxSlots = maxSlotsStr.toIntOrNull() ?: 1
            
            createRoom(name, description, price, capacity, maxSlots, imageUrl, dialog)
        }
        
        dialog.show()
    }
    
    private fun showEditRoomDialog(room: AdminRoomData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_room, null)
        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_room_name)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_room_description)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.et_room_price)
        val etCapacity = dialogView.findViewById<TextInputEditText>(R.id.et_room_capacity)
        val etMaxSlots = dialogView.findViewById<TextInputEditText>(R.id.et_room_max_slots)
        val etImageUrl = dialogView.findViewById<TextInputEditText>(R.id.et_room_image_url)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)
        
        tvTitle.text = "Chỉnh sửa phòng"
        etName.setText(room.name)
        etDescription.setText(room.description)
        etPrice.setText(room.price.toString())
        etCapacity.setText(room.capacity.toString())
        etMaxSlots.setText(room.maxSlots.toString())
        etImageUrl.setText(room.imageUrl)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val description = etDescription.text?.toString()?.trim() ?: ""
            val priceStr = etPrice.text?.toString()?.trim() ?: ""
            val capacityStr = etCapacity.text?.toString()?.trim() ?: ""
            val maxSlotsStr = etMaxSlots.text?.toString()?.trim() ?: "1"
            val imageUrl = etImageUrl.text?.toString()?.trim() ?: ""
            
            if (name.isEmpty() || priceStr.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val price = priceStr.toDoubleOrNull() ?: 0.0
            val capacity = capacityStr.toIntOrNull() ?: 1
            val maxSlots = maxSlotsStr.toIntOrNull() ?: 1
            
            updateRoom(room.id, name, description, price, capacity, maxSlots, imageUrl, dialog)
        }
        
        dialog.show()
    }
    
    private fun createRoom(name: String, description: String, price: Double, capacity: Int, maxSlots: Int, imageUrl: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                val request = RoomRequest(name, description, price, capacity, imageUrl, maxSlots)
                val response = ApiClient.adminApiService.createRoom(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.get("success") == true) {
                        Toast.makeText(this@AdminRoomsActivity, "Thêm phòng thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadRooms() // Reload list
                    } else {
                        val errorMsg = body?.get("error")?.toString() ?: "Thêm phòng thất bại"
                        Toast.makeText(this@AdminRoomsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Thêm phòng thất bại"
                    Toast.makeText(this@AdminRoomsActivity, errorBody, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateRoom(roomId: String, name: String, description: String, price: Double, capacity: Int, maxSlots: Int, imageUrl: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                val request = RoomRequest(name, description, price, capacity, imageUrl, maxSlots)
                val response = ApiClient.adminApiService.updateRoom(roomId, request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.get("success") == true) {
                        Toast.makeText(this@AdminRoomsActivity, "Cập nhật phòng thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadRooms() // Reload list
                    } else {
                        val errorMsg = body?.get("error")?.toString() ?: "Cập nhật phòng thất bại"
                        Toast.makeText(this@AdminRoomsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Cập nhật phòng thất bại"
                    Toast.makeText(this@AdminRoomsActivity, errorBody, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeleteConfirmDialog(room: AdminRoomData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa phòng")
            .setMessage("Bạn có chắc chắn muốn xóa phòng \"${room.name}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteRoom(room.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun deleteRoom(roomId: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.adminApiService.deleteRoom(roomId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.get("success") == true) {
                        Toast.makeText(this@AdminRoomsActivity, "Xóa phòng thành công!", Toast.LENGTH_SHORT).show()
                        adapter.removeRoom(roomId)
                    } else {
                        val errorMsg = body?.get("error")?.toString() ?: "Xóa phòng thất bại"
                        Toast.makeText(this@AdminRoomsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Xóa phòng thất bại"
                    Toast.makeText(this@AdminRoomsActivity, errorBody, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
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
