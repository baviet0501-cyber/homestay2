package com.example.homestay.utils

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Utility class for input validation
 * 
 * Tính năng 2: Validation mạnh mẽ
 * - Password strength validation
 * - Email format validation
 * - Phone number validation (Vietnam format)
 * - Input sanitization
 */
object InputValidator {
    
    /**
     * Password strength levels
     */
    enum class PasswordStrength {
        WEAK,       // Yếu - dưới yêu cầu
        MEDIUM,     // Trung bình - đạt yêu cầu tối thiểu
        STRONG,     // Mạnh - vượt yêu cầu
        VERY_STRONG // Rất mạnh - an toàn cao
    }

    /**
     * Validate và kiểm tra độ mạnh mật khẩu
     * @param password Mật khẩu cần kiểm tra
     * @return Pair<Boolean, PasswordStrength> - (isValid, strength)
     */
    fun validatePassword(password: String): Pair<Boolean, PasswordStrength> {
        if (password.length < 8) {
            return Pair(false, PasswordStrength.WEAK)
        }

        var strength = 0
        
        // Kiểm tra độ dài
        if (password.length >= 8) strength++
        if (password.length >= 12) strength++
        
        // Kiểm tra chữ hoa
        if (password.any { it.isUpperCase() }) strength++
        
        // Kiểm tra chữ thường
        if (password.any { it.isLowerCase() }) strength++
        
        // Kiểm tra số
        if (password.any { it.isDigit() }) strength++
        
        // Kiểm tra ký tự đặc biệt
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (password.any { it in specialChars }) strength++
        
        // Kiểm tra không chứa thông tin cá nhân (tên, email phổ biến)
        val commonWords = listOf("password", "123456", "qwerty", "admin", "user")
        if (!commonWords.any { password.contains(it, ignoreCase = true) }) {
            strength++
        }

        return when {
            strength <= 3 -> Pair(false, PasswordStrength.WEAK)
            strength <= 4 -> Pair(true, PasswordStrength.MEDIUM)
            strength <= 5 -> Pair(true, PasswordStrength.STRONG)
            else -> Pair(true, PasswordStrength.VERY_STRONG)
        }
    }

    /**
     * Kiểm tra mật khẩu có đáp ứng yêu cầu tối thiểu không
     * Yêu cầu:
     * - Tối thiểu 8 ký tự
     * - Có chữ hoa, chữ thường, số và ký tự đặc biệt
     */
    fun isPasswordValid(password: String): Boolean {
        val (isValid, _) = validatePassword(password)
        if (!isValid) return false

        // Kiểm tra yêu cầu bắt buộc
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val hasSpecial = password.any { it in specialChars }

        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    /**
     * Lấy thông điệp lỗi cho mật khẩu
     */
    fun getPasswordErrorMessage(password: String): String {
        val (isValid, strength) = validatePassword(password)
        
        if (!isValid) {
            return "Mật khẩu phải có ít nhất 8 ký tự"
        }

        val missingRequirements = mutableListOf<String>()
        
        if (!password.any { it.isUpperCase() }) {
            missingRequirements.add("chữ hoa")
        }
        if (!password.any { it.isLowerCase() }) {
            missingRequirements.add("chữ thường")
        }
        if (!password.any { it.isDigit() }) {
            missingRequirements.add("số")
        }
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (!password.any { it in specialChars }) {
            missingRequirements.add("ký tự đặc biệt (!@#$%^&*()_+-=[]{}|;:,.<>?)")
        }

        return if (missingRequirements.isNotEmpty()) {
            "Mật khẩu cần có: ${missingRequirements.joinToString(", ")}"
        } else {
            "Mật khẩu hợp lệ"
        }
    }

    /**
     * Validate email format
     * @param email Email cần kiểm tra
     * @return true nếu email hợp lệ
     */
    fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) return false
        
        // Kiểm tra định dạng cơ bản
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false
        }

        // Kiểm tra độ dài
        if (email.length > 254) return false // RFC 5321

        // Kiểm tra không có khoảng trắng
        if (email.contains(" ")) return false

        // Kiểm tra có @ và domain
        val parts = email.split("@")
        if (parts.size != 2) return false
        
        val localPart = parts[0]
        val domain = parts[1]

        // Local part không được rỗng và không quá 64 ký tự
        if (localPart.isEmpty() || localPart.length > 64) return false

        // Domain phải có ít nhất 1 dấu chấm
        if (!domain.contains(".")) return false

        // Domain không được bắt đầu hoặc kết thúc bằng dấu chấm
        if (domain.startsWith(".") || domain.endsWith(".")) return false

        return true
    }

    /**
     * Validate số điện thoại Việt Nam
     * Định dạng: 
     * - 10 số: 0xxxxxxxxx
     * - 11 số: 0xxxxxxxxxx hoặc +84xxxxxxxxx
     * @param phone Số điện thoại cần kiểm tra
     * @return true nếu số điện thoại hợp lệ
     */
    fun validatePhoneNumber(phone: String): Boolean {
        if (phone.isEmpty()) return false

        // Loại bỏ khoảng trắng và dấu gạch ngang
        val cleaned = phone.replace(" ", "").replace("-", "")

        // Kiểm tra định dạng Việt Nam
        // 10 số: bắt đầu bằng 0
        val pattern10 = Pattern.compile("^0[0-9]{9}$")
        
        // 11 số: bắt đầu bằng 0
        val pattern11 = Pattern.compile("^0[0-9]{10}$")
        
        // 12 số: bắt đầu bằng +84
        val patternInternational = Pattern.compile("^\\+84[0-9]{9,10}$")

        return pattern10.matcher(cleaned).matches() ||
               pattern11.matcher(cleaned).matches() ||
               patternInternational.matcher(cleaned).matches()
    }

    /**
     * Format số điện thoại về dạng chuẩn (10 số với 0 đầu)
     */
    fun normalizePhoneNumber(phone: String): String {
        val cleaned = phone.replace(" ", "").replace("-", "")
        
        // Nếu bắt đầu bằng +84, đổi thành 0
        if (cleaned.startsWith("+84")) {
            return "0" + cleaned.substring(3)
        }
        
        return cleaned
    }

    /**
     * Validate họ và tên
     * - Không được rỗng
     * - Tối thiểu 2 ký tự
     * - Tối đa 50 ký tự
     * - Chỉ chứa chữ cái, khoảng trắng và một số ký tự đặc biệt
     */
    fun validateFullName(name: String): Boolean {
        if (name.isEmpty() || name.length < 2) return false
        if (name.length > 50) return false
        
        // Chỉ chứa chữ cái, khoảng trắng, dấu nháy đơn, dấu gạch ngang
        val pattern = Pattern.compile("^[\\p{L}\\s'\\-]+$")
        return pattern.matcher(name.trim()).matches()
    }

    /**
     * Sanitize input - loại bỏ các ký tự nguy hiểm
     * @param input Input cần sanitize
     * @return Input đã được sanitize
     */
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }
}

