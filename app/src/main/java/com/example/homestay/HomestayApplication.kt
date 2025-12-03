package com.example.homestay

import android.app.Application
import com.example.homestay.data.database.HomestayDatabase
import com.example.homestay.data.repository.AuthRepository
import com.example.homestay.data.repository.BookingRepository
import com.example.homestay.data.repository.HomestayRepository

class HomestayApplication : Application() {
    val database by lazy { HomestayDatabase.getDatabase(this) }
    
    // HomestayRepository - cho rooms, bookings, favorites (local data)
    val repository by lazy {
        HomestayRepository(
            database.roomDao(),
            database.slotDao(),
            database.bookingDao(),
            database.userDao(),
            database.favoriteDao()
        )
    }
    
    // AuthRepository - cho authentication (gọi backend API)
    val authRepository by lazy {
        AuthRepository(database.userDao())
    }
    
    // BookingRepository - cho booking (gọi backend API)
    val bookingRepository by lazy {
        BookingRepository(
            database.bookingDao(),
            database.roomDao(),
            database.userDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Rooms sẽ được sync từ backend API khi app khởi động (MainActivity)
        // Không cần seed local database nữa vì dữ liệu đã có trong MongoDB
    }
}

