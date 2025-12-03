package com.example.homestay.data.api

import android.os.Build

/**
 * API Configuration
 * 
 * Để sử dụng:
 * 1. Tìm IP address của máy tính (chạy ipconfig trên Windows)
 * 2. Thay YOUR_IP_ADDRESS bằng IP thực tế (ví dụ: 192.168.1.100)
 * 3. Đảm bảo server đang chạy trên port 3000
 * 4. Đảm bảo điện thoại và máy tính cùng mạng WiFi
 */
object ApiConfig {
    // URL cho điện thoại thật (thay YOUR_IP_ADDRESS bằng IP thực tế của máy tính)
    private const val BASE_URL_PHONE = "http://172.16.72.226:3000/api/"
    
    // URL cho emulator (emulator có thể truy cập máy host qua 10.0.2.2)
    private const val BASE_URL_EMULATOR = "http://10.0.2.2:3000/api/"
    
    // Tự động chọn URL dựa trên thiết bị (emulator hoặc điện thoại thật)
    val BASE_URL: String
        get() = if (isEmulator()) {
            BASE_URL_EMULATOR
        } else {
            BASE_URL_PHONE
        }
    
    // Hàm detect emulator
    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
    }
    
    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 30L // seconds
    const val WRITE_TIMEOUT = 30L // seconds
}

