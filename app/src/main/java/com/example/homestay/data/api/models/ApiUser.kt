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

// Rate Limit Info
data class RateLimitInfo(
    val remaining: Int? = null,
    val reset: String? = null // ISO timestamp
)

// Auth Response - Đồng bộ với backend
data class AuthResponse(
    val success: Boolean,
    val user: ApiUser? = null,
    val error: String? = null,
    val message: String? = null,
    // Rate limiting và account lockout info từ backend
    val failedAttempts: Int? = null,
    val remainingAttempts: Int? = null,
    val maxAttempts: Int? = null,
    val locked: Boolean? = null,
    val permanent: Boolean? = null, // Khóa vĩnh viễn
    val lockedUntil: String? = null, // ISO timestamp
    val secondsRemaining: Long? = null,
    val minutesRemaining: Int? = null,
    val rateLimit: RateLimitInfo? = null
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

