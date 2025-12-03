package com.example.homestay.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["userId", "roomId"], unique = true)]
)
data class Favorite(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val roomId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

