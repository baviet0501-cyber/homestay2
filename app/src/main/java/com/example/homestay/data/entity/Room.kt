package com.example.homestay.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class Room(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mongoId: String? = null, // MongoDB ObjectId - để sync với backend
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val location: String,
    val address: String,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val amenities: String, // JSON string hoặc comma-separated
    val maxGuests: Int = 2,
    val roomType: String, // "Phòng đơn", "Phòng đôi", "Phòng gia đình"
    val area: Int = 0, // diện tích m²
    val maxSlots: Int = 1, // Số slot tối đa của phòng (giới hạn số booking cùng lúc)
    val isAvailable: Boolean = true
)

