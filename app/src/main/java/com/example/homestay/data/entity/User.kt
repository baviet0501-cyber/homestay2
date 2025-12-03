package com.example.homestay.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true), Index(value = ["phone"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val phone: String,
    val password: String, // Trong thực tế nên hash password
    val fullName: String,
    val createdAt: Long = System.currentTimeMillis()
)

