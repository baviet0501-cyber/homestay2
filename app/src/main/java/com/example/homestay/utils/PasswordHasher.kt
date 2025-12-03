package com.example.homestay.utils

import org.mindrot.jbcrypt.BCrypt

/**
 * Utility class for password hashing and verification using BCrypt
 * 
 * - Tính năng 1: Mã hóa mật khẩu
 * - Sử dụng BCrypt với salt tự động
 * - Work factor: 12 (tùy chỉnh theo yêu cầu)
 * - Bảo mật mật khẩu ngay cả khi database bị rò rỉ
 */
object PasswordHasher {
    // BCrypt work factor - số lần hash (12 = 2^12 iterations)
    // Tăng work factor sẽ tăng thời gian hash nhưng tăng bảo mật
    private const val BCRYPT_ROUNDS = 12

    /**
     * Hash a plain text password
     * @param plainPassword Plain text password
     * @return Hashed password string
     */
    fun hash(plainPassword: String): String {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS))
    }

    /**
     * Verify if a plain password matches the hashed password
     * @param plainPassword Plain text password to verify
     * @param hashedPassword Hashed password to compare against
     * @return true if password matches, false otherwise
     */
    fun verify(plainPassword: String, hashedPassword: String): Boolean {
        return try {
            BCrypt.checkpw(plainPassword, hashedPassword)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a string is a valid BCrypt hash
     * @param hash String to check
     * @return true if it's a valid BCrypt hash format
     */
    fun isValidHash(hash: String): Boolean {
        return hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$") || hash.startsWith("\$2y\$")
    }
}

