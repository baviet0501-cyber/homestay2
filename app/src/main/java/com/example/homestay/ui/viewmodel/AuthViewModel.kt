package com.example.homestay.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.homestay.data.entity.User
import com.example.homestay.data.repository.AuthRepository
import com.example.homestay.utils.InputValidator
import com.example.homestay.utils.RateLimiter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class AuthResult(
    val success: Boolean,
    val user: User? = null,
    val mongoUserId: String? = null, // MongoDB ObjectId - để gọi API update
    val message: String? = null,
    val remainingAttempts: Int? = null, // Số lần còn lại (cho Rate Limiting)
    val lockedUntil: Long? = null // Timestamp khi hết khóa
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val context: Context? = null
) : ViewModel() {
    private val _loginResult = MutableStateFlow<AuthResult?>(null)
    val loginResult: StateFlow<AuthResult?> = _loginResult

    private val _registerResult = MutableStateFlow<AuthResult?>(null)
    val registerResult: StateFlow<AuthResult?> = _registerResult

    /**
     * Login với Rate Limiting và validation
     *  Tính năng 2, 3: Validation mạnh mẽ + Rate Limiting
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                // Validation email
                if (!InputValidator.validateEmail(email)) {
                    _loginResult.value = AuthResult(
                        success = false,
                        message = "Email không hợp lệ"
                    )
                    return@launch
                }

                // Rate Limiting - Kiểm tra xem có thể đăng nhập không
                if (context != null) {
                    val (canAttempt, lockedUntil) = RateLimiter.canAttemptLogin(context, email)
                    if (!canAttempt && lockedUntil != null) {
                        val secondsRemaining = RateLimiter.getLockedSecondsRemaining(context, email)
                        val timeMessage = if (secondsRemaining < 60) {
                            "$secondsRemaining giây"
                        } else {
                            val minutes = secondsRemaining / 60
                            val seconds = secondsRemaining % 60
                            if (seconds > 0) "$minutes phút $seconds giây" else "$minutes phút"
                        }
                        _loginResult.value = AuthResult(
                            success = false,
                            message = "Vui lòng thử lại sau $timeMessage.",
                            lockedUntil = lockedUntil
                        )
                        return@launch
                    }
                }

                // Thử đăng nhập với backend API
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    val authData = result.getOrNull()
                    // Đăng nhập thành công - reset rate limit
                    if (context != null) {
                        RateLimiter.recordSuccess(context, email)
                    }
                    _loginResult.value = AuthResult(
                        success = true, 
                        user = authData?.user,
                        mongoUserId = authData?.mongoUserId
                    )
                } else {
                    // Đăng nhập thất bại - lấy thông tin từ backend response
                    val exception = result.exceptionOrNull()
                    val errorMsg = exception?.message ?: "Đăng nhập thất bại"
                    
                    // Kiểm tra xem có phải LoginException với AuthResponse không
                    val loginException = exception as? com.example.homestay.data.repository.LoginException
                    val authResponse = loginException?.authResponse
                    
                    if (authResponse != null && context != null) {
                        // Đồng bộ thông tin từ backend vào RateLimiter
                        RateLimiter.syncFromBackend(
                            context = context,
                            identifier = email,
                            failedAttempts = authResponse.failedAttempts,
                            lockedUntil = authResponse.lockedUntil,
                            remainingAttempts = authResponse.remainingAttempts
                        )
                        
                        // Sử dụng thông tin từ backend
                        val remainingAttempts = authResponse.remainingAttempts ?: 0
                        val lockedUntilTimestamp = authResponse.lockedUntil?.let {
                            try {
                                java.time.Instant.parse(it).toEpochMilli()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        val message = when {
                            authResponse.locked == true && authResponse.permanent == true -> {
                                authResponse.message ?: "Tài khoản đã bị khóa vĩnh viễn. Vui lòng liên hệ admin để mở khóa."
                            }
                            authResponse.locked == true -> {
                                val minutes = authResponse.minutesRemaining ?: 0
                                authResponse.message ?: "Tài khoản đã bị khóa. Vui lòng thử lại sau $minutes phút."
                            }
                            remainingAttempts > 0 -> {
                                authResponse.message ?: "$errorMsg. Còn $remainingAttempts lần thử."
                            }
                            else -> {
                                authResponse.message ?: errorMsg
                            }
                        }
                        
                        _loginResult.value = AuthResult(
                            success = false,
                            message = message,
                            remainingAttempts = remainingAttempts,
                            lockedUntil = lockedUntilTimestamp
                        )
                    } else {
                        // Fallback: chỉ sử dụng local rate limiter nếu KHÔNG có thông tin từ backend
                        // (tức là lỗi mạng hoặc server không phản hồi)
                        if (context != null) {
                            // Chỉ record failure nếu đây là lỗi xác thực thực sự (không phải lỗi mạng)
                            // Nếu errorMsg chứa "401" hoặc "password" thì mới là lỗi đăng nhập
                            val isAuthError = errorMsg.contains("401") || 
                                            errorMsg.contains("password", ignoreCase = true) ||
                                            errorMsg.contains("email", ignoreCase = true) ||
                                            errorMsg.contains("không đúng", ignoreCase = true) ||
                                            errorMsg.contains("không tồn tại", ignoreCase = true)
                            
                            if (isAuthError) {
                                val (remainingAttempts, newLockedUntil) = RateLimiter.recordFailure(context, email)
                                val message = if (remainingAttempts > 0) {
                                    "$errorMsg. Còn $remainingAttempts lần thử."
                                } else {
                                    "Đã vượt quá số lần đăng nhập sai. Tài khoản đã bị khóa."
                                }
                                _loginResult.value = AuthResult(
                                    success = false,
                                    message = message,
                                    remainingAttempts = remainingAttempts,
                                    lockedUntil = newLockedUntil
                                )
                            } else {
                                // Lỗi mạng hoặc lỗi khác - không record failure
                                _loginResult.value = AuthResult(
                                    success = false,
                                    message = errorMsg
                                )
                            }
                        } else {
                            _loginResult.value = AuthResult(
                                success = false,
                                message = errorMsg
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _loginResult.value = AuthResult(success = false, message = "Lỗi: ${e.message}")
            }
        }
    }

    /**
     * Register với validation mạnh mẽ
     *  Tính năng 2: Validation mạnh mẽ
     */
    fun register(fullName: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            try {
                // Validation họ và tên
                if (!InputValidator.validateFullName(fullName)) {
                    _registerResult.value = AuthResult(
                        success = false,
                        message = "Họ và tên không hợp lệ (tối thiểu 2 ký tự, tối đa 50 ký tự)"
                    )
                    return@launch
                }

                // Validation email
                if (!InputValidator.validateEmail(email)) {
                    _registerResult.value = AuthResult(
                        success = false,
                        message = "Email không hợp lệ"
                    )
                    return@launch
                }

                // Validation số điện thoại
                if (!InputValidator.validatePhoneNumber(phone)) {
                    _registerResult.value = AuthResult(
                        success = false,
                        message = "Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam (10-11 số, bắt đầu bằng 0)"
                    )
                    return@launch
                }

                // Validation mật khẩu mạnh
                val (isPasswordValid, passwordStrength) = InputValidator.validatePassword(password)
                if (!isPasswordValid || !InputValidator.isPasswordValid(password)) {
                    val errorMsg = InputValidator.getPasswordErrorMessage(password)
                    _registerResult.value = AuthResult(
                        success = false,
                        message = errorMsg
                    )
                    return@launch
                }

                // Normalize phone number
                val normalizedPhone = InputValidator.normalizePhoneNumber(phone)
                val sanitizedFullName = InputValidator.sanitizeInput(fullName)

                // Gọi backend API để đăng ký
                val result = authRepository.register(
                    email = email,
                    phone = normalizedPhone,
                    password = password,
                    fullName = sanitizedFullName
                )

                if (result.isSuccess) {
                    val authData = result.getOrNull()
                    _registerResult.value = AuthResult(
                        success = true, 
                        user = authData?.user,
                        mongoUserId = authData?.mongoUserId,
                        message = "Đăng ký thành công"
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Đăng ký thất bại"
                    _registerResult.value = AuthResult(
                        success = false, 
                        message = errorMsg
                    )
                }
            } catch (e: Exception) {
                _registerResult.value = AuthResult(success = false, message = "Lỗi: ${e.message}")
            }
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

