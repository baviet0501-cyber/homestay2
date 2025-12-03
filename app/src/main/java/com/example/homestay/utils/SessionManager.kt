package com.example.homestay.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Session Manager - Quản lý session đăng nhập đơn giản
 * 
 * Lưu trữ thông tin user đã đăng nhập (userId, email, name)
 * Không có token, expiration hay auto-logout
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "HomestaySession"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_MONGO_USER_ID = "mongo_user_id" // MongoDB ObjectId
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }

    /**
     * Lưu session
     * @param userId: Local Room DB user ID (Long)
     * @param mongoUserId: MongoDB ObjectId (String) - để gọi API update
     * @param email: Email
     * @param name: Full name
     */
    fun saveSession(userId: Long, mongoUserId: String? = null, email: String, name: String) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_MONGO_USER_ID, mongoUserId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            apply()
        }
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }
    
    fun getMongoUserId(): String? {
        return prefs.getString(KEY_MONGO_USER_ID, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Kiểm tra đã đăng nhập chưa
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != -1L
    }

    /**
     * Xóa session
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

