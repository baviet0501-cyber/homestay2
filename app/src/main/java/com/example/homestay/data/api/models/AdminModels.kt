package com.example.homestay.data.api.models

// Admin Login Request
data class AdminLoginRequest(
    val username: String,
    val password: String
)

// Admin Response
data class AdminData(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val role: String,
    val createdAt: Long
)

data class AdminLoginResponse(
    val success: Boolean,
    val admin: AdminData? = null,
    val error: String? = null
)

// Room Admin Response
data class AdminRoomData(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val capacity: Int,
    val imageUrl: String,
    val maxSlots: Int,
    val createdAt: Long
)

data class AdminRoomsResponse(
    val success: Boolean,
    val rooms: List<AdminRoomData>? = null,
    val error: String? = null
)

// Create/Update Room Request
data class RoomRequest(
    val name: String,
    val description: String,
    val price: Double,
    val capacity: Int,
    val imageUrl: String,
    val maxSlots: Int
)

// User Admin Response  
data class AdminUserData(
    val id: String,
    val email: String,
    val phone: String,
    val fullName: String,
    val createdAt: Long
)

data class AdminUsersResponse(
    val success: Boolean,
    val users: List<AdminUserData>? = null,
    val error: String? = null
)

// Booking Admin Response
data class AdminBookingUser(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String
)

data class AdminBookingRoom(
    val id: String,
    val name: String,
    val price: Double
)

data class AdminBookingData(
    val id: String,
    val user: AdminBookingUser?,
    val room: AdminBookingRoom?,
    val checkInDate: Long,
    val checkOutDate: Long,
    val guestCount: Int,
    val totalPrice: Double,
    val status: String,
    val paymentMethod: String?,
    val createdAt: Long
)

data class AdminBookingsResponse(
    val success: Boolean,
    val bookings: List<AdminBookingData>? = null,
    val error: String? = null
)

// Update Booking Status Request
data class UpdateBookingStatusRequest(
    val status: String // pending, confirmed, cancelled, completed
)

// Generic Response
data class GenericResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

