package com.example.homestay.data.api

import com.example.homestay.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApiService {
    
    // ==================== Admin Auth ====================
    
    @POST("admin/login")
    suspend fun adminLogin(@Body request: AdminLoginRequest): Response<AdminLoginResponse>
    
    // ==================== User Management ====================
    
    @GET("admin/users")
    suspend fun getUsers(): Response<AdminUsersResponse>
    
    @DELETE("admin/users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): Response<Map<String, Any>>
    
    // ==================== Room Management ====================
    
    @GET("admin/rooms")
    suspend fun getRooms(): Response<AdminRoomsResponse>
    
    @POST("admin/rooms")
    suspend fun createRoom(@Body request: RoomRequest): Response<Map<String, Any>>
    
    @PUT("admin/rooms/{id}")
    suspend fun updateRoom(
        @Path("id") roomId: String,
        @Body request: RoomRequest
    ): Response<Map<String, Any>>
    
    @DELETE("admin/rooms/{id}")
    suspend fun deleteRoom(@Path("id") roomId: String): Response<Map<String, Any>>
    
    // ==================== Booking Management ====================
    
    @GET("admin/bookings")
    suspend fun getBookings(): Response<AdminBookingsResponse>
    
    @PUT("admin/bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Path("id") bookingId: String,
        @Body request: UpdateBookingStatusRequest
    ): Response<Map<String, Any>>
    
    @DELETE("admin/bookings/{id}")
    suspend fun deleteBooking(@Path("id") bookingId: String): Response<Map<String, Any>>
}

