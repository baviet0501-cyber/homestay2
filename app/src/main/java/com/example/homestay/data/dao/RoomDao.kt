package com.example.homestay.data.dao

import androidx.room.*
import com.example.homestay.data.entity.Room
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY id ASC")
    fun getAllRooms(): Flow<List<Room>>

    @Query("SELECT * FROM rooms WHERE isAvailable = 1 ORDER BY id ASC")
    fun getAvailableRooms(): Flow<List<Room>>

    @Query("SELECT * FROM rooms WHERE name LIKE '%' || :query || '%' AND isAvailable = 1 ORDER BY id ASC")
    fun searchRoomsByName(query: String): Flow<List<Room>>

    @Query("SELECT * FROM rooms WHERE (name LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%') AND isAvailable = 1 ORDER BY id ASC")
    fun searchRooms(query: String): Flow<List<Room>>

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    suspend fun getRoomById(roomId: Long): Room?

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    fun getRoomByIdFlow(roomId: Long): Flow<Room?>

    @Query("SELECT * FROM rooms WHERE mongoId = :mongoId")
    suspend fun getRoomByMongoId(mongoId: String): Room?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: Room): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<Room>)

    @Update
    suspend fun updateRoom(room: Room)

    @Delete
    suspend fun deleteRoom(room: Room)

    @Query("DELETE FROM rooms")
    suspend fun deleteAllRooms()
}

