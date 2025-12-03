package com.example.homestay.data.api.models

import com.example.homestay.data.entity.Booking

/**
 * API models for Booking-related requests/responses
 */

// Create Booking Request
data class CreateBookingRequest(
    val roomId: String, // MongoDB ObjectId
    val checkInDate: Long, // Timestamp
    val checkOutDate: Long, // Timestamp
    val guestCount: Int,
    val totalPrice: Double,
    val status: String = "pending",
    val paymentMethod: String? = null,
    val slotId: String? = null // MongoDB ObjectId, optional
)

// Update Booking Request
data class UpdateBookingRequest(
    val status: String? = null,
    val paymentMethod: String? = null
)

// Booking Response from API (MongoDB ObjectId as String)
data class ApiBooking(
    val id: String, // MongoDB ObjectId
    val userId: String, // MongoDB ObjectId
    val roomId: String, // MongoDB ObjectId
    val checkInDate: Long, // Timestamp
    val checkOutDate: Long, // Timestamp
    val guestCount: Int,
    val totalPrice: Double,
    val status: String,
    val paymentMethod: String?,
    val slotId: String? = null, // MongoDB ObjectId, optional
    val createdAt: Long
)

// Booking Response with room info (from GET /api/bookings)
data class BookingWithRoomInfo(
    val id: String,
    val userId: String,
    val roomId: String,
    val checkInDate: Long,
    val checkOutDate: Long,
    val guestCount: Int,
    val totalPrice: Double,
    val status: String,
    val paymentMethod: String?,
    val slotId: String?,
    val createdAt: Long,
    val room: RoomInfo? = null // Populated room info
)

data class RoomInfo(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val imageUrl: String?
)

// Create Booking Response
data class CreateBookingResponse(
    val success: Boolean,
    val booking: ApiBooking? = null,
    val error: String? = null
)

// Extension function to convert ApiBooking to Room Booking entity
fun ApiBooking.toEntity(localId: Long = 0, localRoomId: Long = 0, localUserId: Long = 0, localSlotId: Long? = null): Booking {
    return Booking(
        id = localId, // Room uses Long, sẽ được auto-generate hoặc map từ cache
        roomId = localRoomId, // Cần map từ MongoDB roomId sang local roomId
        slotId = localSlotId, // Cần map từ MongoDB slotId sang local slotId
        userId = localUserId, // Cần map từ MongoDB userId sang local userId
        checkInDate = this.checkInDate,
        checkOutDate = this.checkOutDate,
        guestCount = this.guestCount,
        totalPrice = this.totalPrice,
        status = this.status,
        paymentMethod = this.paymentMethod,
        createdAt = this.createdAt
    )
}

