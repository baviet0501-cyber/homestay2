package com.example.homestay.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "slots",
    indices = [Index(value = ["roomId"])]
)
data class Slot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roomId: Long,
    val slotNumber: Int, // Số slot trong phòng (1, 2, 3...)
    val slotName: String, // "Giường đơn", "Giường đôi", "Sofa bed"
    val isAvailable: Boolean = true,
    val price: Double? = null // Giá riêng cho slot này (nếu khác giá phòng)
)

