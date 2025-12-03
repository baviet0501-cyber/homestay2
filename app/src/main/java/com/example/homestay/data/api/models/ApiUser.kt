package com.example.homestay.data.api.models

import com.example.homestay.data.entity.User

/**
 * API models for User-related requests/responses
 */

// Login Request
data class LoginRequest(
    val email: String,
    val password: String
)

// Register Request
data class RegisterRequest(
    val email: String,
    val phone: String,
    val password: String,
    val fullName: String
)

// User Response from API (MongoDB ObjectId as String)
data class ApiUser(
    val id: String, // MongoDB ObjectId
    val email: String,
    val phone: String,
    val fullName: String,
    val createdAt: Long
)

// Auth Response
data class AuthResponse(
    val success: Boolean,
    val user: ApiUser? = null,
    val error: String? = null,
    val message: String? = null
)

// Update User Request (chỉ fullName và password)
data class UpdateUserRequest(
    val fullName: String,
    val password: String? = null // Optional - chỉ update nếu user muốn đổi
)

// Extension function to convert ApiUser to Room User entity
fun ApiUser.toEntity(localId: Long = 0): User {
    return User(
        id = localId, // Room uses Long, sẽ được auto-generate
        email = this.email,
        phone = this.phone,
        password = "", // Không lưu password từ API
        fullName = this.fullName,
        createdAt = this.createdAt
    )
}

