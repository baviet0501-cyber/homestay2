package com.example.homestay.data.api.models

import com.example.homestay.data.entity.Room

/**
 * API models for Room-related requests/responses
 */

// Room Response from API (MongoDB ObjectId as String)
data class ApiRoom(
    val id: String, // MongoDB ObjectId
    val name: String,
    val description: String,
    val price: Double,
    val capacity: Int,
    val imageUrl: String,
    val maxSlots: Int,
    val location: String = "",
    val address: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val amenities: String = "",
    val roomType: String = "",
    val area: Int = 0,
    val createdAt: Long
)

// Extension function to convert ApiRoom to Room entity
fun ApiRoom.toEntity(localId: Long = 0): Room {
    return Room(
        id = localId, // Room uses Long, sẽ được map từ local DB
        mongoId = this.id, // MongoDB ObjectId
        name = this.name,
        description = this.description,
        price = this.price,
        imageUrl = this.imageUrl,
        location = this.location,
        address = this.address,
        rating = this.rating.toFloat(),
        reviewCount = this.reviewCount,
        amenities = this.amenities,
        maxGuests = this.capacity,
        roomType = this.roomType,
        area = this.area,
        maxSlots = this.maxSlots,
        isAvailable = true // Mặc định available
    )
}

