package com.example.homestay.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

/**
 * Rate Limiter để chống brute force attack
 * 
 *  Rate Limiting
 * - Giới hạn số lần đăng nhập sai
 * - Khóa tài khoản tạm thời sau nhiều lần sai
 * - Tăng thời gian khóa theo số lần sai
 */
object RateLimiter {
    private const val PREFS_NAME = "RateLimiterPrefs"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts_"
    private const val KEY_LOCKED_UNTIL = "locked_until_"
    private const val KEY_LAST_ATTEMPT = "last_attempt_"
    private const val KEY_LOCK_COUNT = "lock_count_" // Số lần đã bị khóa (tích lũy)

    // Giới hạn số lần sai - Đồng bộ với backend
    private const val MAX_FAILED_ATTEMPTS = 5
    
    // Thời gian khóa - Khóa vĩnh viễn (100 năm)
    private const val LOCKOUT_DURATION_SECONDS = 100L * 365 * 24 * 60 * 60 // 100 năm

    /**
     * Kiểm tra xem có thể thử đăng nhập không
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     * @return Pair<Boolean, Long?> - (canAttempt, lockedUntilTimestamp) 
     *         - canAttempt: true nếu có thể thử, false nếu bị khóa
     *         - lockedUntilTimestamp: timestamp khi hết khóa (null nếu không bị khóa)
     */
    fun canAttemptLogin(context: Context, identifier: String): Pair<Boolean, Long?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockedUntil = prefs.getLong("$KEY_LOCKED_UNTIL$identifier", 0)
        
        // Nếu đã hết thời gian khóa
        if (lockedUntil > 0 && System.currentTimeMillis() >= lockedUntil) {
            // Chỉ xóa locked_until, giữ lại failed_attempts và lock_count để tích lũy
            // failed_attempts sẽ được reset về 0 khi bắt đầu đếm lại trong recordFailure()
            prefs.edit()
                .remove("$KEY_LOCKED_UNTIL$identifier")
                .apply()
            return Pair(true, null)
        }
        
        // Nếu đang bị khóa
        if (lockedUntil > 0 && System.currentTimeMillis() < lockedUntil) {
            return Pair(false, lockedUntil)
        }
        
        return Pair(true, null)
    }

    /**
     * Ghi nhận đăng nhập thành công - reset failed attempts
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     */
    fun recordSuccess(context: Context, identifier: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Reset tất cả khi đăng nhập thành công
        prefs.edit()
            .remove("$KEY_FAILED_ATTEMPTS$identifier")
            .remove("$KEY_LOCKED_UNTIL$identifier")
            .remove("$KEY_LAST_ATTEMPT$identifier")
            .remove("$KEY_LOCK_COUNT$identifier") // Reset lock count
            .apply()
    }

    /**
     * Ghi nhận đăng nhập thất bại
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     * @return Pair<Int, Long?> - (remainingAttempts, lockedUntilTimestamp)
     *         - remainingAttempts: Số lần còn lại (0 nếu bị khóa)
     *         - lockedUntilTimestamp: Timestamp khi hết khóa (null nếu chưa khóa)
     */         
    fun recordFailure(context: Context, identifier: String): Pair<Int, Long?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Kiểm tra xem đang bị khóa không
        val lockedUntil = prefs.getLong("$KEY_LOCKED_UNTIL$identifier", 0)
        val isCurrentlyLocked = lockedUntil > 0 && System.currentTimeMillis() < lockedUntil
        
        // Nếu đang bị khóa, không cho phép đếm thêm
        if (isCurrentlyLocked) {
            val remaining = lockedUntil - System.currentTimeMillis()
            val secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(remaining) + 1
            return Pair(0, lockedUntil)
        }
        
        // Nếu không bị khóa, kiểm tra xem đã hết khóa trước đó chưa
        // Nếu đã hết khóa (lockedUntil > 0 nhưng đã quá thời gian), reset failed_attempts để đếm lại
        var currentAttempts = prefs.getInt("$KEY_FAILED_ATTEMPTS$identifier", 0)
        val editor = prefs.edit()
        
        // Nếu đã có lockedUntil trước đó nhưng đã hết khóa, reset failed_attempts về 0 để đếm lại
        if (lockedUntil > 0 && !isCurrentlyLocked) {
            // Đã hết khóa, reset failed_attempts về 0 để đếm lại cho lần khóa tiếp theo
            // Nhưng giữ lại lock_count để tích lũy thời gian khóa
            currentAttempts = 0
            editor.putInt("$KEY_FAILED_ATTEMPTS$identifier", 0) // Reset ngay trong editor
        }
        
        val newAttempts = currentAttempts + 1
        editor.putInt("$KEY_FAILED_ATTEMPTS$identifier", newAttempts)
        editor.putLong("$KEY_LAST_ATTEMPT$identifier", System.currentTimeMillis())
        
        // Nếu đạt giới hạn (5 lần sai), khóa tài khoản vĩnh viễn
        // Set lockedUntil thành 100 năm sau (coi như vĩnh viễn)
        if (newAttempts >= MAX_FAILED_ATTEMPTS) {
            val newLockedUntil = System.currentTimeMillis() + 
                (LOCKOUT_DURATION_SECONDS * 1000) // Khóa vĩnh viễn
            
            editor.putLong("$KEY_LOCKED_UNTIL$identifier", newLockedUntil)
            editor.apply()
            
            return Pair(0, newLockedUntil)
        }
        
        editor.apply()
        val remainingAttempts = MAX_FAILED_ATTEMPTS - newAttempts
        return Pair(remainingAttempts, null)
    }

    /**
     * Lấy số lần đăng nhập sai còn lại
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     * @return Số lần còn lại
     */
    fun getRemainingAttempts(context: Context, identifier: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val failedAttempts = prefs.getInt("$KEY_FAILED_ATTEMPTS$identifier", 0)
        return (MAX_FAILED_ATTEMPTS - failedAttempts).coerceAtLeast(0)
    }

    /**
     * Lấy thời gian còn lại bị khóa (tính bằng giây)
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     * @return Số giây còn lại (0 nếu không bị khóa)
     */
    fun getLockedSecondsRemaining(context: Context, identifier: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockedUntil = prefs.getLong("$KEY_LOCKED_UNTIL$identifier", 0)
        
        if (lockedUntil <= 0) return 0
        
        val remaining = lockedUntil - System.currentTimeMillis()
        return if (remaining > 0) {
            TimeUnit.MILLISECONDS.toSeconds(remaining) + 1 // +1 để làm tròn lên
        } else {
            0
        }
    }

    /**
     * Lấy thời gian còn lại bị khóa (tính bằng phút) - Giữ lại để tương thích
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     * @return Số phút còn lại (0 nếu không bị khóa)
     */
    fun getLockedMinutesRemaining(context: Context, identifier: String): Long {
        val seconds = getLockedSecondsRemaining(context, identifier)
        return (seconds / 60) + if (seconds % 60 > 0) 1 else 0 // Làm tròn lên
    }

    /**
     * Đồng bộ thông tin rate limit từ backend response
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     * @param failedAttempts Số lần đăng nhập thất bại từ backend
     * @param lockedUntil Timestamp khi hết khóa từ backend (ISO string hoặc timestamp)
     * @param remainingAttempts Số lần còn lại từ backend
     */
    fun syncFromBackend(
        context: Context, 
        identifier: String,
        failedAttempts: Int? = null,
        lockedUntil: Any? = null, // Có thể là String (ISO) hoặc Long (timestamp)
        remainingAttempts: Int? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Sync failed attempts
        if (failedAttempts != null) {
            editor.putInt("$KEY_FAILED_ATTEMPTS$identifier", failedAttempts)
        }
        
        // Sync locked until
        if (lockedUntil != null) {
            val lockedUntilTimestamp = when (lockedUntil) {
                is String -> {
                    // Parse ISO string to timestamp
                    try {
                        java.time.Instant.parse(lockedUntil).toEpochMilli()
                    } catch (e: Exception) {
                        0L
                    }
                }
                is Long -> lockedUntil
                is Number -> lockedUntil.toLong()
                else -> 0L
            }
            
            if (lockedUntilTimestamp > 0) {
                editor.putLong("$KEY_LOCKED_UNTIL$identifier", lockedUntilTimestamp)
            } else {
                editor.remove("$KEY_LOCKED_UNTIL$identifier")
            }
        }
        
        editor.apply()
    }
    
    /**
     * Lấy số lần đăng nhập thất bại hiện tại
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     * @return Số lần thất bại
     */
    fun getFailedAttempts(context: Context, identifier: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("$KEY_FAILED_ATTEMPTS$identifier", 0)
    }
    
    /**
     * Reset rate limit cho một identifier (dùng cho admin hoặc test)
     * @param context Context
     * @param identifier Email hoặc identifier duy nhất
     */
    fun reset(context: Context, identifier: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("$KEY_FAILED_ATTEMPTS$identifier")
            .remove("$KEY_LOCKED_UNTIL$identifier")
            .remove("$KEY_LAST_ATTEMPT$identifier")
            .remove("$KEY_LOCK_COUNT$identifier")
            .apply()
    }
}

