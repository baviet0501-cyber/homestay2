package com.example.homestay.data.repository

import android.util.Log
import com.example.homestay.data.api.ApiClient
import com.example.homestay.data.api.models.ApiRoom
import com.example.homestay.data.api.models.toEntity
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
import com.example.homestay.utils.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HomestayRepository(
    private val roomDao: RoomDao,
    private val slotDao: SlotDao,
    private val bookingDao: BookingDao,
    private val userDao: UserDao,
    private val favoriteDao: FavoriteDao
) {
    // Room operations
    fun getAllRooms(): Flow<List<Room>> = roomDao.getAllRooms()
    fun getAvailableRooms(): Flow<List<Room>> = roomDao.getAvailableRooms()
    fun searchRoomsByName(query: String): Flow<List<Room>> = roomDao.searchRoomsByName(query)
    fun searchRooms(query: String): Flow<List<Room>> = roomDao.searchRooms(query)
    suspend fun getRoomById(roomId: Long): Room? = roomDao.getRoomById(roomId)
    fun getRoomByIdFlow(roomId: Long): Flow<Room?> = roomDao.getRoomByIdFlow(roomId)
    suspend fun insertRoom(room: Room): Long = roomDao.insertRoom(room)
    suspend fun insertRooms(rooms: List<Room>) = roomDao.insertRooms(rooms)
    suspend fun updateRoom(room: Room) = roomDao.updateRoom(room)
    suspend fun deleteRoom(room: Room) = roomDao.deleteRoom(room)

    // Slot operations
    fun getSlotsByRoomId(roomId: Long): Flow<List<Slot>> = slotDao.getSlotsByRoomId(roomId)
    fun getAvailableSlotsByRoomId(roomId: Long): Flow<List<Slot>> = slotDao.getAvailableSlotsByRoomId(roomId)
    suspend fun getSlotById(slotId: Long): Slot? = slotDao.getSlotById(slotId)
    suspend fun insertSlot(slot: Slot): Long = slotDao.insertSlot(slot)
    suspend fun insertSlots(slots: List<Slot>) = slotDao.insertSlots(slots)
    suspend fun updateSlot(slot: Slot) = slotDao.updateSlot(slot)
    suspend fun deleteSlot(slot: Slot) = slotDao.deleteSlot(slot)

    // Booking operations
    fun getAllBookings(): Flow<List<Booking>> = bookingDao.getAllBookings()
    fun getBookingsByUserId(userId: Long): Flow<List<Booking>> = bookingDao.getBookingsByUserId(userId)
    fun getBookingsByRoomId(roomId: Long): Flow<List<Booking>> = bookingDao.getBookingsByRoomId(roomId)
    suspend fun getBookingById(bookingId: Long): Booking? = bookingDao.getBookingById(bookingId)
    suspend fun insertBooking(booking: Booking): Long = bookingDao.insertBooking(booking)
    suspend fun updateBooking(booking: Booking) = bookingDao.updateBooking(booking)
    suspend fun deleteBooking(booking: Booking) = bookingDao.deleteBooking(booking)
    suspend fun countOverlappingBookings(roomId: Long, checkInDate: Long, checkOutDate: Long): Int =
        bookingDao.countOverlappingBookings(roomId, checkInDate, checkOutDate)

    // User operations
    /**
     * Login với BCrypt password verification
     * Mã hóa mật khẩu
     */
    suspend fun login(email: String, password: String): User? {
        val user = userDao.getUserByEmailForLogin(email) ?: return null
        // Verify password bằng BCrypt
        if (PasswordHasher.verify(password, user.password)) {
            return user
        }
        return null
    }
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun getUserByPhone(phone: String): User? = userDao.getUserByPhone(phone)
    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)
    fun getUserByIdFlow(userId: Long): Flow<User?> = userDao.getUserByIdFlow(userId)
    /**
     * Insert user với password đã được hash bằng BCrypt
     * Tính năng 1: Mã hóa mật khẩu
     */
    suspend fun insertUser(user: User): Long {
        // Hash password trước khi lưu
        val hashedPassword = PasswordHasher.hash(user.password)
        val userWithHashedPassword = user.copy(password = hashedPassword)
        return userDao.insertUser(userWithHashedPassword)
    }
    /**
     * Update user - hash password mới nếu được cung cấp
     * Tính năng 1: Mã hóa mật khẩu
     */
    suspend fun updateUser(user: User) {
        val existingUser = userDao.getUserById(user.id)
        if (existingUser != null) {
            // Nếu password mới được cung cấp và khác với password cũ, hash nó
            val updatedPassword = if (user.password != existingUser.password && 
                !PasswordHasher.isValidHash(user.password)) {
                PasswordHasher.hash(user.password)
            } else {
                user.password // Giữ nguyên nếu đã là hash hoặc không đổi
            }
            val updatedUser = user.copy(password = updatedPassword)
            userDao.updateUser(updatedUser)
        } else {
            userDao.updateUser(user)
        }
    }
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    // Favorite operations
    suspend fun getFavorite(userId: Long, roomId: Long): Favorite? = favoriteDao.getFavorite(userId, roomId)
    fun getFavoriteRoomIds(userId: Long): Flow<List<Long>> = favoriteDao.getFavoriteRoomIds(userId)
    fun getFavoritesByUserId(userId: Long): Flow<List<Favorite>> = favoriteDao.getFavoritesByUserId(userId)
    suspend fun insertFavorite(favorite: Favorite): Long = favoriteDao.insertFavorite(favorite)
    suspend fun deleteFavorite(favorite: Favorite) = favoriteDao.deleteFavorite(favorite)
    suspend fun deleteFavorite(userId: Long, roomId: Long) = favoriteDao.deleteFavorite(userId, roomId)
    suspend fun isFavorite(userId: Long, roomId: Long): Boolean = favoriteDao.isFavorite(userId, roomId) > 0
    
    /**
     * Clean up duplicate rooms - xóa các rooms duplicate dựa trên mongoId
     */
    suspend fun cleanupDuplicateRooms() {
        try {
            val allRooms = getAllRooms().first()
            val roomsByMongoId = allRooms.filter { it.mongoId != null }
                .groupBy { it.mongoId }
            
            var duplicateCount = 0
            for ((mongoId, duplicateRooms) in roomsByMongoId) {
                if (duplicateRooms.size > 1) {
                    // Giữ lại room đầu tiên (id nhỏ nhất), xóa các rooms còn lại
                    val sortedRooms = duplicateRooms.sortedBy { it.id }
                    val roomsToDelete = sortedRooms.drop(1)
                    duplicateCount += roomsToDelete.size
                    for (room in roomsToDelete) {
                        deleteRoom(room)
                        Log.d("HomestayRepository", "Deleted duplicate room: id=${room.id}, mongoId=$mongoId, name=${room.name}")
                    }
                }
            }
            if (duplicateCount > 0) {
                Log.d("HomestayRepository", "Cleaned up $duplicateCount duplicate rooms")
            }
        } catch (e: Exception) {
            Log.e("HomestayRepository", "Error cleaning up duplicate rooms: ${e.message}", e)
        }
    }
    
    /**
     * Sync rooms từ backend API vào local DB
     * Map rooms từ MongoDB vào local DB, update hoặc insert rooms
     * Xóa các rooms không có trong API (để tránh duplicate)
     */
    suspend fun syncRoomsFromAPI(): Boolean {
        return try {
            Log.d("HomestayRepository", "Syncing rooms from API...")
            val response = ApiClient.apiService.getRooms()
            
            if (response.isSuccessful) {
                val apiRooms = response.body() ?: emptyList()
                Log.d("HomestayRepository", "Received ${apiRooms.size} rooms from API")
                
                // Lấy tất cả rooms hiện tại trong local DB
                val localRooms = getAllRooms().first()
                val apiRoomIds = apiRooms.map { it.id }.toSet()
                
                // Bước 1: Xóa tất cả rooms không có mongoId (old seed data)
                val oldRoomsWithoutMongoId = localRooms.filter { it.mongoId == null }
                if (oldRoomsWithoutMongoId.isNotEmpty()) {
                    Log.d("HomestayRepository", "Deleting ${oldRoomsWithoutMongoId.size} old rooms without mongoId")
                    for (room in oldRoomsWithoutMongoId) {
                        deleteRoom(room)
                    }
                }
                
                // Bước 2: Xóa duplicate rooms - giữ lại room đầu tiên cho mỗi mongoId
                val currentRooms = getAllRooms().first()
                val roomsByMongoId = currentRooms.filter { it.mongoId != null }
                    .groupBy { it.mongoId }
                
                // Xóa các rooms duplicate (giữ lại room đầu tiên)
                var duplicateCount = 0
                for ((mongoId, duplicateRooms) in roomsByMongoId) {
                    if (duplicateRooms.size > 1) {
                        // Giữ lại room đầu tiên, xóa các rooms còn lại
                        val roomsToDelete = duplicateRooms.drop(1)
                        duplicateCount += roomsToDelete.size
                        for (room in roomsToDelete) {
                            deleteRoom(room)
                            Log.d("HomestayRepository", "Deleted duplicate room: ${room.id} with mongoId: $mongoId")
                        }
                    }
                }
                if (duplicateCount > 0) {
                    Log.d("HomestayRepository", "Deleted $duplicateCount duplicate rooms")
                }
                
                // Bước 3: Lấy lại danh sách rooms sau khi clean up
                val cleanRooms = getAllRooms().first()
                
                // Bước 4: Sync rooms từ API
                for (apiRoom in apiRooms) {
                    // Tìm room trong local DB - chỉ tìm theo mongoId
                    val existingRoom = cleanRooms.find { 
                        it.mongoId == apiRoom.id
                    }
                    
                    if (existingRoom != null) {
                        // Update room đã có với data từ API (đầy đủ các fields)
                        val updatedRoom = existingRoom.copy(
                            mongoId = apiRoom.id,
                            name = apiRoom.name,
                            description = apiRoom.description,
                            price = apiRoom.price,
                            imageUrl = apiRoom.imageUrl,
                            maxSlots = apiRoom.maxSlots,
                            maxGuests = apiRoom.capacity,
                            location = apiRoom.location,
                            address = apiRoom.address,
                            rating = apiRoom.rating.toFloat(),
                            reviewCount = apiRoom.reviewCount,
                            amenities = apiRoom.amenities,
                            roomType = apiRoom.roomType,
                            area = apiRoom.area
                        )
                        updateRoom(updatedRoom)
                        Log.d("HomestayRepository", "Updated room: ${apiRoom.name} with mongoId: ${apiRoom.id}")
                    } else {
                        // Insert room mới nếu chưa có
                        val newRoom = apiRoom.toEntity()
                        val newId = insertRoom(newRoom)
                        Log.d("HomestayRepository", "Inserted new room: ${apiRoom.name} with localId: $newId, mongoId: ${apiRoom.id}")
                    }
                }
                
                // Bước 5: Xóa các rooms có mongoId nhưng không có trong API (đã bị xóa trên backend)
                val finalRooms = getAllRooms().first()
                val roomsToDelete = finalRooms.filter { room ->
                    room.mongoId != null && !apiRoomIds.contains(room.mongoId)
                }
                
                if (roomsToDelete.isNotEmpty()) {
                    Log.d("HomestayRepository", "Deleting ${roomsToDelete.size} rooms not in API anymore")
                    for (room in roomsToDelete) {
                        deleteRoom(room)
                    }
                }
                
                Log.d("HomestayRepository", "Rooms sync completed successfully. Total: ${apiRooms.size}")
                true
            } else {
                Log.e("HomestayRepository", "Failed to sync rooms: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("HomestayRepository", "Error syncing rooms: ${e.message}", e)
            false
        }
    }
}

