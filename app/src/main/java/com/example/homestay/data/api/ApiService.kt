package com.example.homestay.data.api

import com.example.homestay.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service - Định nghĩa các endpoint của backend
 */
interface ApiService {
    
    // ==================== Authentication ====================
    
    /**
     * POST /auth/register
     * Đăng ký tài khoản mới
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    /**
     * POST /auth/login
     * Đăng nhập
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    // ==================== Users ====================
    
    /**
     * GET /users/:id
     * Lấy thông tin user theo ID
     */
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: String): Response<AuthResponse>
    
    /**
     * PUT /users/:id
     * Cập nhật thông tin user (CHỈ fullName và password)
     */
    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<AuthResponse>
    
    // ==================== Rooms ====================
    
    /**
     * GET /rooms
     * Lấy danh sách rooms từ backend
     */
    @GET("rooms")
    suspend fun getRooms(): Response<List<ApiRoom>>
    
    // ==================== Bookings ====================
    
    /**
     * GET /bookings
     * Lấy danh sách bookings của user
     * Backend sẽ lấy userId từ authenticateUser middleware (header hoặc query)
     */
    @GET("bookings")
    suspend fun getBookings(
        @Query("userId") userId: String
    ): Response<List<BookingWithRoomInfo>>
    
    /**
     * POST /bookings
     * Tạo booking mới
     * Backend sẽ lấy userId từ authenticateUser middleware
     */
    @POST("bookings")
    suspend fun createBooking(
        @Body request: CreateBookingRequest
    ): Response<CreateBookingResponse>
    
    /**
     * PUT /bookings/:id
     * Cập nhật booking (status, paymentMethod)
     * Backend sẽ lấy userId từ authenticateUser middleware
     */
    @PUT("bookings/{id}")
    suspend fun updateBooking(
        @Path("id") bookingId: String,
        @Body request: UpdateBookingRequest
    ): Response<Map<String, Any>>
}

