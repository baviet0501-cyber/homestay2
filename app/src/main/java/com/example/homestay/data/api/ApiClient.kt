package com.example.homestay.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit Client - Singleton để quản lý API calls
 */
object ApiClient {
    
    // Lazy initialization của Retrofit instance
    private val retrofit: Retrofit by lazy {
        // Logging interceptor để debug API calls
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log full request/response
        }
        
        // User ID interceptor - tự động thêm userId vào header nếu có
        val userIdInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val userId = getCurrentUserId() // Lấy từ SessionManager hoặc global state
            
            val newRequest = if (userId != null) {
                originalRequest.newBuilder()
                    .header("user-id", userId)
                    .build()
            } else {
                originalRequest
            }
            
            chain.proceed(newRequest)
        }
        
        // OkHttp client với timeouts và logging
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(userIdInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
        
        // Retrofit instance
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // API Service instance
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    // Admin API Service instance
    val adminApiService: AdminApiService by lazy {
        retrofit.create(AdminApiService::class.java)
    }
    
    // Current user ID (MongoDB) - được set khi user login
    @Volatile
    private var currentUserId: String? = null
    
    fun setCurrentUserId(userId: String?) {
        currentUserId = userId
    }
    
    private fun getCurrentUserId(): String? = currentUserId
}

