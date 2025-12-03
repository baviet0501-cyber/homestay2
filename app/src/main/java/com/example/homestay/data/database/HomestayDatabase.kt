package com.example.homestay.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room as RoomBuilder
import androidx.room.RoomDatabase
import com.example.homestay.data.dao.BookingDao
import com.example.homestay.data.dao.FavoriteDao
import com.example.homestay.data.dao.RoomDao
import com.example.homestay.data.dao.SlotDao
import com.example.homestay.data.dao.UserDao
import com.example.homestay.data.entity.Booking
import com.example.homestay.data.entity.Favorite
import com.example.homestay.data.entity.Room
import com.example.homestay.data.entity.Slot
import com.example.homestay.data.entity.User

@Database(
    entities = [
        Room::class,
        Slot::class,
        Booking::class,
        User::class,
        Favorite::class
    ],
    version = 7,
    exportSchema = false
)
abstract class HomestayDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun slotDao(): SlotDao
    abstract fun bookingDao(): BookingDao
    abstract fun userDao(): UserDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: HomestayDatabase? = null

        fun getDatabase(context: Context): HomestayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = RoomBuilder.databaseBuilder(
                    context.applicationContext,
                    HomestayDatabase::class.java,
                    "homestay_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
