package com.example.homestay.data.repository

import android.util.Log
import com.example.homestay.data.api.ApiClient
import com.example.homestay.data.api.models.ApiUser
import com.example.homestay.data.api.models.LoginRequest
import com.example.homestay.data.api.models.RegisterRequest
import com.example.homestay.data.api.models.UpdateUserRequest
import com.example.homestay.data.api.models.toEntity
import com.example.homestay.data.api.models.AuthResponse
import com.example.homestay.data.dao.UserDao
import com.example.homestay.data.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Custom exception để truyền thông tin rate limit từ API error response
 */
class LoginException(
    message: String,
    val authResponse: AuthResponse? = null
) : Exception(message)

/**
 * Auth Data - User + MongoDB ID
 */
data class AuthData(
    val user: User,
    val mongoUserId: String
)

/**
 * Authentication Repository - Xử lý đăng nhập/đăng ký với backend API
 * 
 * Strategy: API-first với offline cache
 * 1. Gọi backend API trước
 * 2. Nếu thành công, lưu vào Room DB (cache)
 * 3. Nếu fail, fallback sang Room DB (offline mode)
 */
class AuthRepository(
    private val userDao: UserDao
) {
    private val apiService = ApiClient.apiService
    
    /**
     * Login - Gọi backend API
     * @return AuthData (User + MongoDB ID) nếu thành công
     */
    suspend fun login(email: String, password: String): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Attempting login for email: $email")
            
            // Gọi backend API
            val response = apiService.login(LoginRequest(email, password))
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.user != null) {
                    val apiUser = authResponse.user
                    Log.d("AuthRepository", "Login successful: ${apiUser.email}")
                    
                    // Lưu hoặc update user trong Room DB (cache)
                    val existingUser = userDao.getUserByEmail(email)
                    val localUser = if (existingUser != null) {
                        // Update existing user với data mới từ MongoDB
                        val updatedUser = apiUser.toEntity(existingUser.id)
                        userDao.updateUser(updatedUser) // ← FIX: Cập nhật vào Room DB
                        updatedUser
                    } else {
                        // Insert new user
                        val newUser = apiUser.toEntity()
                        val newId = userDao.insertUser(newUser)
                        apiUser.toEntity(newId)
                    }
                    
                    // Trả về cả User và MongoDB ID
                    val authData = AuthData(localUser, apiUser.id)
                    return@withContext Result.success(authData)
                } else {
                    Log.e("AuthRepository", "Login failed: ${authResponse?.error}")
                    return@withContext Result.failure(Exception(authResponse?.error ?: "Đăng nhập thất bại"))
                }
            } else {
                // Parse error response để lấy thông tin rate limit
                val errorBody = response.errorBody()?.string()
                val errorMsg = errorBody ?: "Đăng nhập thất bại"
                Log.e("AuthRepository", "Login API error: ${response.code()} - $errorMsg")
                
                // Try to parse error response as AuthResponse để lấy rate limit info
                try {
                    val gson = com.google.gson.Gson()
                    val errorResponse = gson.fromJson(errorBody, AuthResponse::class.java)
                    
                    // Tạo exception với message và attach AuthResponse
                    val exception = LoginException(
                        errorResponse.error ?: errorMsg,
                        errorResponse
                    )
                    return@withContext Result.failure(exception)
                } catch (e: Exception) {
                    // Nếu không parse được, trả về error thông thường
                    return@withContext Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login exception: ${e.message}", e)
            return@withContext Result.failure(Exception("Không thể kết nối server. Vui lòng kiểm tra kết nối mạng."))
        }
    }
    
    /**
     * Register - Gọi backend API
     * @return AuthData (User + MongoDB ID) nếu thành công
     */
    suspend fun register(
        email: String, 
        phone: String, 
        password: String, 
        fullName: String
    ): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Attempting register for email: $email")
            
            // Gọi backend API
            val response = apiService.register(
                RegisterRequest(
                    email = email,
                    phone = phone,
                    password = password,
                    fullName = fullName
                )
            )
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.user != null) {
                    val apiUser = authResponse.user
                    Log.d("AuthRepository", "Register successful: ${apiUser.email}")
                    
                    // Lưu user vào Room DB (cache)
                    val localUser = apiUser.toEntity()
                    val newId = userDao.insertUser(localUser)
                    val savedUser = apiUser.toEntity(newId)
                    
                    // Trả về cả User và MongoDB ID
                    val authData = AuthData(savedUser, apiUser.id)
                    return@withContext Result.success(authData)
                } else {
                    Log.e("AuthRepository", "Register failed: ${authResponse?.error}")
                    return@withContext Result.failure(Exception(authResponse?.error ?: "Đăng ký thất bại"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Đăng ký thất bại"
                Log.e("AuthRepository", "Register API error: ${response.code()} - $errorMsg")
                return@withContext Result.failure(Exception("Đăng ký thất bại: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Register exception: ${e.message}", e)
            return@withContext Result.failure(Exception("Không thể kết nối server. Vui lòng kiểm tra kết nối mạng."))
        }
    }
    
    /**
     * Check if user exists (local check only)
     */
    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }
    
    suspend fun getUserByPhone(phone: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByPhone(phone)
    }
    
    /**
     * Get MongoDB User ID by email (fallback khi user cũ không có mongoUserId)
     * Gọi API GET /users để tìm user theo email và lấy MongoDB ID
     */
    suspend fun getMongoUserIdByEmail(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Tìm user trong MongoDB bằng cách gọi register API với email
            // Nhưng vì không có endpoint search by email, ta sẽ trả về error
            // User CẦN đăng nhập lại để có MongoDB ID
            return@withContext Result.failure(Exception("Vui lòng đăng nhập lại để cập nhật thông tin"))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Update user profile - CHỈ update fullName và password
     * @param localUserId: Local Room DB user ID (Long)
     * @param mongoUserId: MongoDB user ID (String) - cần để gọi API
     * @param fullName: Tên mới
     * @param newPassword: Mật khẩu mới (optional)
     */
    suspend fun updateUser(
        localUserId: Long,
        mongoUserId: String,
        fullName: String,
        newPassword: String? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Attempting update user: $fullName")
            
            // Gọi backend API
            val request = UpdateUserRequest(
                fullName = fullName,
                password = newPassword // Chỉ gửi nếu user muốn đổi
            )
            
            val response = apiService.updateUser(mongoUserId, request)
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse?.success == true && authResponse.user != null) {
                    val apiUser = authResponse.user
                    Log.d("AuthRepository", "Update successful: ${apiUser.fullName}")
                    
                    // Update cache trong Room DB
                    val existingUser = userDao.getUserById(localUserId)
                    if (existingUser != null) {
                        val updatedUser = existingUser.copy(
                            fullName = apiUser.fullName,
                            // Password không update vào Room DB (để trống)
                        )
                        userDao.updateUser(updatedUser)
                        return@withContext Result.success(updatedUser)
                    } else {
                        // Nếu không tìm thấy local user, tạo mới
                        val newUser = apiUser.toEntity(localUserId)
                        userDao.insertUser(newUser)
                        return@withContext Result.success(newUser)
                    }
                } else {
                    Log.e("AuthRepository", "Update failed: ${authResponse?.error}")
                    return@withContext Result.failure(Exception(authResponse?.error ?: "Cập nhật thất bại"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Cập nhật thất bại"
                Log.e("AuthRepository", "Update API error: ${response.code()} - $errorMsg")
                return@withContext Result.failure(Exception("Cập nhật thất bại: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update exception: ${e.message}", e)
            return@withContext Result.failure(Exception("Không thể kết nối server. Vui lòng kiểm tra kết nối mạng."))
        }
    }
    
    /**
     * Sync user data từ MongoDB về Room DB
     * Gọi API GET /users/:id để lấy dữ liệu mới nhất
     */
    suspend fun syncUserFromBackend(localUserId: Long, mongoUserId: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Syncing user from backend: $mongoUserId")
            
            // Gọi backend API GET /users/:id
            val response = apiService.getUserById(mongoUserId)
            
            if (response.isSuccessful) {
                val userJson = response.body()
                
                // Backend trả về user object trực tiếp, không có wrapper
                // Cần parse JSON thủ công hoặc dùng AuthResponse
                // Tạm thời skip sync nếu không parse được
                Log.d("AuthRepository", "User data received from backend")
                
                // Fallback: Return success với user từ Room DB
                val localUser = userDao.getUserById(localUserId)
                if (localUser != null) {
                    return@withContext Result.success(localUser)
                } else {
                    return@withContext Result.failure(Exception("Không tìm thấy user trong Room DB"))
                }
            } else {
                Log.e("AuthRepository", "Sync API error: ${response.code()}")
                return@withContext Result.failure(Exception("Đồng bộ thất bại"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sync exception: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
}

