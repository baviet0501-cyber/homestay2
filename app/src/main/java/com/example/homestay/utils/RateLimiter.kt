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

    // Giới hạn số lần sai
    private const val MAX_FAILED_ATTEMPTS = 5
    
    // Thời gian khóa tăng dần theo số lần khóa: 2s, 3s, 5s
    // lockCount = 1: 2 giây, lockCount = 2: 3 giây, lockCount >= 3: 5 giây
    private val LOCK_DURATION_SECONDS = listOf(2L, 3L, 5L)

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
        
        // Nếu đạt giới hạn (5 lần sai), khóa tài khoản
        if (newAttempts >= MAX_FAILED_ATTEMPTS) {
            // Lấy số lần đã bị khóa trước đó (lock count) và tăng lên
            val lockCount = prefs.getInt("$KEY_LOCK_COUNT$identifier", 0) + 1
            editor.putInt("$KEY_LOCK_COUNT$identifier", lockCount)
            
            // Xác định thời gian khóa dựa trên lock count (số lần đã bị khóa)
            // lockCount = 1: 2s (lần khóa đầu tiên)
            // lockCount = 2: 3s (lần khóa thứ 2)  
            // lockCount >= 3: 5s - Max cho các lần sau
            val durationIndex = when {
                lockCount == 1 -> 0 // 2 giây (lần khóa đầu tiên)
                lockCount == 2 -> 1 // 3 giây (lần khóa thứ 2)
                else -> 2 // 5 giây - Max cho các lần sau (lockCount >= 3)
            }
            
            val lockDurationSeconds = LOCK_DURATION_SECONDS[durationIndex.coerceAtMost(LOCK_DURATION_SECONDS.size - 1)]
            
            val newLockedUntil = System.currentTimeMillis() + 
                TimeUnit.SECONDS.toMillis(lockDurationSeconds)
            
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
            .remove("$KEY_LOCK_COUNT$identifier") // Reset lock count
            .apply()
    }
}

