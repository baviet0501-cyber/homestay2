package com.example.homestay.data.repository

import android.util.Log
import com.example.homestay.data.api.ApiClient
import com.example.homestay.data.api.models.*
import com.example.homestay.data.dao.BookingDao
import com.example.homestay.data.dao.RoomDao
import com.example.homestay.data.dao.UserDao
import com.example.homestay.data.entity.Booking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Booking Repository - Xử lý booking với backend API
 * 
 * Strategy: API-first với offline cache
 * 1. Gọi backend API trước
 * 2. Nếu thành công, lưu vào Room DB (cache)
 * 3. Nếu fail, fallback sang Room DB (offline mode)
 */
class BookingRepository(
    private val bookingDao: BookingDao,
    private val roomDao: com.example.homestay.data.dao.RoomDao,
    private val userDao: UserDao
) {
    private val apiService = ApiClient.apiService
    
    /**
     * Create Booking - Gọi backend API
     * @param mongoUserId MongoDB User ID
     * @param localUserId Local User ID (Room DB)
     * @param localRoomId Local Room ID (Room DB)
     * @param mongoRoomId MongoDB Room ID
     * @param request Booking request data
     * @return Result<BookingData> với local Booking entity và MongoDB ID
     */
    suspend fun createBooking(
        mongoUserId: String,
        localUserId: Long,
        localRoomId: Long,
        mongoRoomId: String,
        request: CreateBookingRequest
    ): Result<BookingData> = withContext(Dispatchers.IO) {
        try {
            Log.d("BookingRepository", "Creating booking: roomId=${request.roomId}, userId=$mongoUserId")
            
            // Set userId cho ApiClient để interceptor tự động thêm vào header
            ApiClient.setCurrentUserId(mongoUserId)
            
            // Gọi backend API với userId trong header
            val response = apiService.createBooking(request)
            
            if (response.isSuccessful) {
                val createResponse = response.body()
                if (createResponse?.success == true && createResponse.booking != null) {
                    val apiBooking = createResponse.booking
                    
                    // Map MongoDB IDs sang local IDs và lưu vào Room DB
                    val localSlotId = request.slotId?.let { slotId ->
                        // TODO: Map mongoSlotId sang localSlotId nếu cần
                        null // Tạm thời null
                    }
                    
                    val localBooking = Booking(
                        mongoId = apiBooking.id,
                        roomId = localRoomId,
                        mongoRoomId = apiBooking.roomId,
                        slotId = localSlotId,
                        mongoSlotId = apiBooking.slotId,
                        userId = localUserId,
                        mongoUserId = apiBooking.userId,
                        checkInDate = apiBooking.checkInDate,
                        checkOutDate = apiBooking.checkOutDate,
                        guestCount = apiBooking.guestCount,
                        totalPrice = apiBooking.totalPrice,
                        status = apiBooking.status,
                        paymentMethod = apiBooking.paymentMethod,
                        createdAt = apiBooking.createdAt
                    )
                    
                    val bookingId = bookingDao.insertBooking(localBooking)
                    val savedBooking = localBooking.copy(id = bookingId)
                    
                    Log.d("BookingRepository", "Booking created successfully: mongoId=${apiBooking.id}, localId=$bookingId")
                    
                    return@withContext Result.success(BookingData(savedBooking, apiBooking.id))
                } else {
                    val errorMsg = createResponse?.error ?: "Tạo booking thất bại"
                    Log.e("BookingRepository", "Create booking failed: $errorMsg")
                    return@withContext Result.failure(Exception(errorMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Tạo booking thất bại"
                Log.e("BookingRepository", "Create booking failed: ${response.code()} - $errorBody")
                return@withContext Result.failure(Exception("Tạo booking thất bại: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Create booking error: ${e.message}", e)
            return@withContext Result.failure(Exception("Không thể kết nối server. Vui lòng kiểm tra kết nối mạng."))
        }
    }
    
    /**
     * Get Bookings by User ID - Gọi backend API và sync vào Room DB
     * @param mongoUserId MongoDB User ID
     * @param localUserId Local User ID (Room DB)
     * @return Flow<List<Booking>> từ cache (Room DB)
     */
    fun getBookingsByUserId(mongoUserId: String, localUserId: Long): Flow<List<Booking>> = flow {
        try {
            // Set userId cho ApiClient để interceptor tự động thêm vào header
            ApiClient.setCurrentUserId(mongoUserId)
            
            // Gọi backend API để sync
            val response = apiService.getBookings(mongoUserId)
            
            if (response.isSuccessful) {
                val bookingsList = response.body() ?: emptyList()
                Log.d("BookingRepository", "Loaded ${bookingsList.size} bookings from API")
                
                // Sync vào Room DB
                withContext(Dispatchers.IO) {
                    syncBookingsToLocal(bookingsList, localUserId)
                }
            }
            
            // Emit từ Room DB cache (sau khi đã sync)
            bookingDao.getBookingsByUserId(localUserId).collect { bookings ->
                emit(bookings)
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Get bookings error: ${e.message}", e)
            // Fallback: emit từ Room DB cache
            bookingDao.getBookingsByUserId(localUserId).collect { bookings ->
                emit(bookings)
            }
        }
    }
    
    /**
     * Update Booking - Gọi backend API
     */
    suspend fun updateBooking(
        mongoBookingId: String,
        localBookingId: Long,
        request: UpdateBookingRequest
    ): Result<BookingData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateBooking(mongoBookingId, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.get("success") == true) {
                    // Reload booking từ API để sync
                    // TODO: Implement reload nếu cần
                    val localBooking = bookingDao.getBookingById(localBookingId)
                    if (localBooking != null) {
                        val updatedBooking = localBooking.copy(
                            status = request.status ?: localBooking.status,
                            paymentMethod = request.paymentMethod ?: localBooking.paymentMethod
                        )
                        bookingDao.updateBooking(updatedBooking)
                        return@withContext Result.success(BookingData(updatedBooking, mongoBookingId))
                    }
                }
            }
            
            return@withContext Result.failure(Exception("Cập nhật booking thất bại"))
        } catch (e: Exception) {
            Log.e("BookingRepository", "Update booking error: ${e.message}", e)
            return@withContext Result.failure(Exception("Không thể kết nối server."))
        }
    }
    
    /**
     * Sync bookings từ API vào Room DB
     * Map MongoDB IDs sang local IDs và sync vào Room DB
     */
    private suspend fun syncBookingsToLocal(
        apiBookings: List<BookingWithRoomInfo>,
        localUserId: Long
    ) {
        try {
            Log.d("BookingRepository", "Syncing ${apiBookings.size} bookings to local DB...")
            
            // Lấy tất cả rooms hiện tại để map mongoRoomId sang localRoomId
            val allRooms = roomDao.getAllRooms().first()
            
            // Lấy tất cả bookings hiện tại của user
            val existingBookings = bookingDao.getBookingsByUserId(localUserId).first()
            val bookingsByMongoId = existingBookings.filter { it.mongoId != null }
                .associateBy { it.mongoId!! }
            
            // API booking IDs để track bookings đã sync
            val apiBookingIds = apiBookings.map { it.id }.toSet()
            
            // Xóa bookings không còn trong API (đã bị xóa)
            val bookingsToDelete = existingBookings.filter { booking ->
                booking.mongoId != null && !apiBookingIds.contains(booking.mongoId)
            }
            if (bookingsToDelete.isNotEmpty()) {
                Log.d("BookingRepository", "Deleting ${bookingsToDelete.size} bookings not in API")
                bookingsToDelete.forEach { bookingDao.deleteBooking(it) }
            }
            
            // Sync từng booking
            for (apiBooking in apiBookings) {
                // Tìm local room theo mongoRoomId
                val localRoom = allRooms.find { it.mongoId == apiBooking.roomId }
                if (localRoom == null) {
                    Log.w("BookingRepository", "Room not found for mongoRoomId: ${apiBooking.roomId}, skipping booking ${apiBooking.id}")
                    continue
                }
                
                // Tìm existing booking theo mongoId
                val existingBooking = bookingsByMongoId[apiBooking.id]
                
                if (existingBooking != null) {
                    // Update booking đã có với data mới nhất từ API (đặc biệt là status)
                    val updatedBooking = existingBooking.copy(
                        mongoId = apiBooking.id,
                        roomId = localRoom.id,
                        mongoRoomId = apiBooking.roomId,
                        slotId = existingBooking.slotId, // Giữ nguyên slotId local
                        mongoSlotId = apiBooking.slotId,
                        userId = localUserId,
                        mongoUserId = apiBooking.userId,
                        checkInDate = apiBooking.checkInDate,
                        checkOutDate = apiBooking.checkOutDate,
                        guestCount = apiBooking.guestCount,
                        totalPrice = apiBooking.totalPrice,
                        status = apiBooking.status, // Cập nhật status từ admin
                        paymentMethod = apiBooking.paymentMethod,
                        createdAt = apiBooking.createdAt
                    )
                    bookingDao.updateBooking(updatedBooking)
                    Log.d("BookingRepository", "Updated booking: ${apiBooking.id} with status: ${apiBooking.status}")
                } else {
                    // Insert booking mới
                    val newBooking = Booking(
                        mongoId = apiBooking.id,
                        roomId = localRoom.id,
                        mongoRoomId = apiBooking.roomId,
                        slotId = null, // Slot ID sẽ được map sau nếu cần
                        mongoSlotId = apiBooking.slotId,
                        userId = localUserId,
                        mongoUserId = apiBooking.userId,
                        checkInDate = apiBooking.checkInDate,
                        checkOutDate = apiBooking.checkOutDate,
                        guestCount = apiBooking.guestCount,
                        totalPrice = apiBooking.totalPrice,
                        status = apiBooking.status,
                        paymentMethod = apiBooking.paymentMethod,
                        createdAt = apiBooking.createdAt
                    )
                    bookingDao.insertBooking(newBooking)
                    Log.d("BookingRepository", "Inserted new booking: ${apiBooking.id}")
                }
            }
            
            Log.d("BookingRepository", "Bookings sync completed successfully. Total: ${apiBookings.size}")
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error syncing bookings: ${e.message}", e)
        }
    }
    
    /**
     * Sync bookings từ API - Public function để có thể gọi từ bên ngoài
     */
    suspend fun syncBookingsFromAPI(mongoUserId: String, localUserId: Long): Boolean {
        return try {
            ApiClient.setCurrentUserId(mongoUserId)
            val response = apiService.getBookings(mongoUserId)
            
            if (response.isSuccessful) {
                val bookingsList = response.body() ?: emptyList()
                syncBookingsToLocal(bookingsList, localUserId)
                true
            } else {
                Log.e("BookingRepository", "Failed to sync bookings: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error syncing bookings: ${e.message}", e)
            false
        }
    }
    
    /**
     * Count overlapping bookings - từ Room DB cache
     */
    suspend fun countOverlappingBookings(
        roomId: Long,
        checkInDate: Long,
        checkOutDate: Long
    ): Int = withContext(Dispatchers.IO) {
        bookingDao.countOverlappingBookings(roomId, checkInDate, checkOutDate)
    }
}

/**
 * Booking Data - Booking + MongoDB ID
 */
data class BookingData(
    val booking: Booking,
    val mongoBookingId: String
)

