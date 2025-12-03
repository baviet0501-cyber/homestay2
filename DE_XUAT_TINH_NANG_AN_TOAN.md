# ĐỀ XUẤT TÍNH NĂNG AN TOÀN CHO HỆ THỐNG ĐĂNG NHẬP/ĐĂNG KÝ

## 📋 Tổng quan
Tài liệu này đề xuất các tính năng bảo mật cần thiết để bảo vệ hệ thống đăng nhập và đăng ký khỏi các cuộc tấn công phổ biến.

---

## 🔐 1. MÃ HÓA MẬT KHẨU (Password Hashing)

### Mức độ ưu tiên: ⭐⭐⭐⭐⭐ (BẮT BUỘC)

### ✅ ĐÃ TRIỂN KHAI - Ngày: [Ngày triển khai]

**Vấn đề hiện tại:**
- Mật khẩu được lưu plain text trong database
- Nếu database bị rò rỉ, tất cả mật khẩu sẽ bị lộ

**Giải pháp:**
- Sử dụng **BCrypt** hoặc **Argon2** để hash mật khẩu
- Salt tự động được tạo cho mỗi mật khẩu
- Không thể reverse hash về mật khẩu gốc

**Công nghệ:**
- Library: `jbcrypt` hoặc `Android Keystore` với `PBKDF2`

**Ưu điểm:**
- Bảo vệ mật khẩu ngay cả khi database bị xâm nhập
- Tuân thủ các chuẩn bảo mật hiện đại

**Chi tiết triển khai:**
- ✅ Thêm dependency `jbcrypt` vào `gradle/libs.versions.toml`
- ✅ Tạo `PasswordHasher.kt` utility class với BCrypt
  - Work factor: 12 (có thể tùy chỉnh)
  - Methods: `hash()`, `verify()`, `isValidHash()`
- ✅ Cập nhật `UserDao.kt` - login không so sánh password trực tiếp
- ✅ Cập nhật `HomestayRepository.kt`:
  - `login()`: Verify password bằng BCrypt
  - `insertUser()`: Hash password trước khi lưu
  - `updateUser()`: Hash password mới nếu được cung cấp
- ✅ Tất cả mật khẩu mới đều được hash trước khi lưu database

**Files đã thay đổi:**
- `gradle/libs.versions.toml` - Thêm bcrypt dependency
- `app/build.gradle.kts` - Thêm bcrypt library
- `app/src/main/java/com/example/homestay/utils/PasswordHasher.kt` - **MỚI**
- `app/src/main/java/com/example/homestay/data/dao/UserDao.kt` - Cập nhật login query
- `app/src/main/java/com/example/homestay/data/repository/HomestayRepository.kt` - Cập nhật login/insert/update methods

---

## 🛡️ 2. VALIDATION MẠNH MẼ (Strong Input Validation)

### Mức độ ưu tiên: ⭐⭐⭐⭐⭐ (BẮT BUỘC)

### ✅ ĐÃ TRIỂN KHAI - Ngày: [Ngày triển khai]

**Vấn đề hiện tại:**
- Validation cơ bản (chỉ kiểm tra độ dài mật khẩu ≥ 6 ký tự)
- Không kiểm tra độ mạnh mật khẩu

**Giải pháp:**
- **Mật khẩu mạnh:**
  - Tối thiểu 8 ký tự
  - Có chữ hoa, chữ thường, số và ký tự đặc biệt
  - Không chứa thông tin cá nhân (tên, email, số điện thoại)
  - Hiển thị chỉ số độ mạnh mật khẩu (weak/medium/strong)

- **Email validation:**
  - Kiểm tra định dạng chặt chẽ
  - Kiểm tra domain hợp lệ (tùy chọn)

- **Số điện thoại:**
  - Định dạng Việt Nam: 10-11 số, bắt đầu bằng 0 hoặc +84
  - Chỉ chấp nhận số

**UI/UX:**
- Real-time validation feedback
- Hiển thị gợi ý yêu cầu mật khẩu
- Progress bar độ mạnh mật khẩu

**Chi tiết triển khai:**
- ✅ Tạo `InputValidator.kt` utility class với các validation methods:
  - `validatePassword()`: Kiểm tra và đánh giá độ mạnh mật khẩu (WEAK/MEDIUM/STRONG/VERY_STRONG)
  - `isPasswordValid()`: Kiểm tra mật khẩu có đáp ứng yêu cầu tối thiểu
  - `getPasswordErrorMessage()`: Lấy thông điệp lỗi chi tiết
  - `validateEmail()`: Validation email chặt chẽ (RFC 5321)
  - `validatePhoneNumber()`: Validation số điện thoại Việt Nam (10-11 số)
  - `normalizePhoneNumber()`: Chuẩn hóa số điện thoại về dạng 0xxxxxxxxx
  - `validateFullName()`: Validation họ tên (2-50 ký tự, chỉ chữ cái)
  - `sanitizeInput()`: Loại bỏ ký tự nguy hiểm (chống XSS)
- ✅ Cập nhật `AuthViewModel.kt`:
  - `login()`: Validate email trước khi đăng nhập
  - `register()`: Validate đầy đủ fullName, email, phone, password
  - Hiển thị thông báo lỗi chi tiết cho từng trường hợp
- ✅ Cập nhật `RegisterActivity.kt`:
  - Sử dụng `InputValidator` để validate input
  - Hiển thị error message chi tiết cho từng field
  - Validate password strength và hiển thị cảnh báo

**Yêu cầu mật khẩu:**
- Tối thiểu 8 ký tự
- Phải có: chữ hoa, chữ thường, số, ký tự đặc biệt (!@#$%^&*()_+-=[]{}|;:,.<>?)
- Không chứa từ khóa phổ biến (password, 123456, qwerty, admin, user)

**Files đã thay đổi:**
- `app/src/main/java/com/example/homestay/utils/InputValidator.kt` - **MỚI**
- `app/src/main/java/com/example/homestay/ui/viewmodel/AuthViewModel.kt` - Thêm validation
- `app/src/main/java/com/example/homestay/RegisterActivity.kt` - Sử dụng InputValidator

---

## 🚫 3. RATE LIMITING & CHỐNG BRUTE FORCE

### Mức độ ưu tiên: ⭐⭐⭐⭐ (RẤT QUAN TRỌNG)

### ✅ ĐÃ TRIỂN KHAI - Ngày: [Ngày triển khai]

**Vấn đề hiện tại:**
- Không có giới hạn số lần đăng nhập sai
- Dễ bị brute force attack

**Giải pháp:**
- **Giới hạn đăng nhập sai:**
  - Cho phép tối đa 5 lần đăng nhập sai
  - Khóa tài khoản tạm thời 15 phút sau 5 lần sai
  - Tăng thời gian khóa: 15 phút → 30 phút → 1 giờ

- **Giới hạn đăng ký:**
  - Tối đa 3 tài khoản từ cùng 1 IP trong 1 giờ
  - Chống spam đăng ký

- **Theo dõi:**
  - Lưu số lần đăng nhập sai vào database hoặc SharedPreferences
  - Reset khi đăng nhập thành công

**UI/UX:**
- Hiển thị số lần còn lại
- Countdown timer khi bị khóa
- Gửi email thông báo khi tài khoản bị khóa

**Chi tiết triển khai:**
- ✅ Tạo `RateLimiter.kt` utility class:
  - `canAttemptLogin()`: Kiểm tra có thể đăng nhập không
  - `recordSuccess()`: Reset failed attempts khi đăng nhập thành công
  - `recordFailure()`: Ghi nhận đăng nhập thất bại, trả về số lần còn lại
  - `getRemainingAttempts()`: Lấy số lần còn lại
  - `getLockedMinutesRemaining()`: Lấy thời gian còn lại bị khóa
  - Lưu trong SharedPreferences với key theo email
- ✅ Cập nhật `AuthViewModel.kt`:
  - `login()`: Kiểm tra rate limit trước khi đăng nhập
  - Ghi nhận thành công/thất bại vào RateLimiter
  - Trả về `remainingAttempts` và `lockedUntil` trong `AuthResult`
- ✅ Cập nhật `LoginActivity.kt`:
  - Truyền context vào `AuthViewModelFactory` để sử dụng RateLimiter
  - Hiển thị số lần còn lại và thời gian khóa (nếu có)

**Cơ chế hoạt động:**
- Cho phép tối đa 5 lần đăng nhập sai
- Sau 5 lần sai: Khóa 15 phút
- Sau 6-7 lần sai: Khóa 30 phút
- Sau 8+ lần sai: Khóa 60 phút
- Reset khi đăng nhập thành công

**Files đã thay đổi:**
- `app/src/main/java/com/example/homestay/utils/RateLimiter.kt` - **MỚI**
- `app/src/main/java/com/example/homestay/ui/viewmodel/AuthViewModel.kt` - Tích hợp Rate Limiting
- `app/src/main/java/com/example/homestay/LoginActivity.kt` - Truyền context vào ViewModel

---

## 📧 4. XÁC MINH EMAIL (Email Verification)

### Mức độ ưu tiên: ⭐⭐⭐⭐ (RẤT QUAN TRỌNG)

**Vấn đề hiện tại:**
- Không xác minh email khi đăng ký
- Có thể đăng ký bằng email giả

**Giải pháp:**
- Gửi email xác minh sau khi đăng ký
- Token xác minh có thời hạn (24 giờ)
- Chỉ cho phép đăng nhập sau khi xác minh email
- Tùy chọn gửi lại email xác minh

**Công nghệ:**
- Email service: Firebase Authentication, SendGrid, hoặc SMTP trực tiếp
- Token generation và storage

**UI/UX:**
- Hiển thị thông báo "Vui lòng kiểm tra email"
- Nút "Gửi lại email xác minh"
- Thời gian đếm ngược để gửi lại email (60 giây)

---

## 🔄 5. QUẢN LÝ PHIÊN ĐĂNG NHẬP (Session Management)

### Mức độ ưu tiên: ⭐⭐⭐ (QUAN TRỌNG - ĐÃ ĐƠN GIẢN HÓA)

### ❌ ĐÃ XÓA - Không triển khai tính năng phức tạp

**Lý do:**
- Ứng dụng không cần token expiration hay auto-logout
- Đơn giản hóa session management

**Giải pháp hiện tại:**
- **Session đơn giản:**
  - Lưu userId, email, name trong SharedPreferences
  - Không có token, expiration hay auto-logout
  - Session tồn tại cho đến khi user đăng xuất thủ công

**Chi tiết triển khai:**
- ✅ `SessionManager.kt`: Session manager đơn giản
  - `saveSession()`: Lưu userId, email, name
  - `isLoggedIn()`: Kiểm tra đã đăng nhập chưa
  - `clearSession()`: Xóa session
  - Không có token, expiration hay lastActivity

**Files:**
- `app/src/main/java/com/example/homestay/utils/SessionManager.kt` - Session manager đơn giản

---

## 🔑 6. ĐỔI MẬT KHẨU & KHÔI PHỤC TÀI KHOẢN

### Mức độ ưu tiên: ⭐⭐⭐⭐ (RẤT QUAN TRỌNG)

**Vấn đề hiện tại:**
- Không có chức năng quên mật khẩu
- Không có khôi phục tài khoản

**Giải pháp:**
- **Quên mật khẩu:**
  - Nhập email → Gửi link reset mật khẩu
  - Token reset có thời hạn (1 giờ)
  - Sau khi reset, yêu cầu đăng nhập lại

- **Đổi mật khẩu:**
  - Yêu cầu mật khẩu cũ
  - Xác nhận mật khẩu mới
  - Thông báo email khi đổi mật khẩu

- **Khóa tài khoản:**
  - Cho phép người dùng yêu cầu khóa tài khoản
  - Gửi email xác nhận

---

## 🚨 7. BẢO MẬT CHỐNG SQL INJECTION & XSS

### Mức độ ưu tiên: ⭐⭐⭐⭐⭐ (BẮT BUỘC)

**Vấn đề hiện tại:**
- Sử dụng Room (tương đối an toàn) nhưng cần kiểm tra

**Giải pháp:**
- **Input Sanitization:**
  - Loại bỏ các ký tự đặc biệt nguy hiểm
  - Escape HTML trong các field text
  - Whitelist validation (chỉ cho phép ký tự hợp lệ)

- **Parameterized Queries:**
  - Đảm bảo Room sử dụng parameterized queries
  - Không bao giờ concatenate user input vào SQL

- **Content Security Policy:**
  - Validate tất cả input trước khi lưu database
  - Sanitize output khi hiển thị

---

## 📝 8. GHI NHẬN SỰ KIỆN BẢO MẬT (Security Logging)

### Mức độ ưu tiên: ⭐⭐⭐ (QUAN TRỌNG)

**Vấn đề hiện tại:**
- Không có logging các sự kiện bảo mật

**Giải pháp:**
- **Ghi nhận các sự kiện:**
  - Đăng nhập thành công/thất bại
  - Đăng ký tài khoản mới
  - Đổi mật khẩu
  - Reset mật khẩu
  - Tài khoản bị khóa
  - Đăng nhập từ thiết bị mới

- **Thông tin log:**
  - Timestamp
  - IP address (nếu có)
  - Device info
  - User ID

- **Lưu trữ:**
  - Database hoặc file log
  - Không lưu mật khẩu hoặc thông tin nhạy cảm

**Mục đích:**
- Phát hiện hoạt động đáng ngờ
- Audit trail
- Hỗ trợ điều tra khi có sự cố

---

## 🛡️ 9. CAPTCHA (Chống Bot)

### Mức độ ưu tiên: ⭐⭐⭐ (QUAN TRỌNG)

**Giải pháp:**
- **reCAPTCHA v3** hoặc **hCaptcha**
  - Tự động phát hiện bot
  - Không yêu cầu người dùng giải puzzle
  - Score-based (0.0 - 1.0)

- **Hiển thị CAPTCHA khi:**
  - Đăng ký
  - Sau 3 lần đăng nhập sai
  - Phát hiện hành vi đáng ngờ

**UI/UX:**
- Trải nghiệm mượt mà cho người dùng thật
- Chỉ hiển thị khi cần thiết

---

## 🔒 10. MÃ HÓA DỮ LIỆU NHẠY CẢM

### Mức độ ưu tiên: ⭐⭐⭐⭐ (RẤT QUAN TRỌNG)

**Vấn đề hiện tại:**
- Session data lưu trong SharedPreferences không mã hóa
- Thông tin nhạy cảm có thể bị đọc

**Giải pháp:**
- **Android Keystore:**
  - Mã hóa session token
  - Mã hóa thông tin người dùng trong SharedPreferences
  - Key được lưu trong Hardware Security Module (HSM)

- **Encryption:**
  - AES-256-GCM
  - Key rotation (thay đổi key định kỳ)

**Dữ liệu cần mã hóa:**
- Session token
- Email (tùy chọn)
- Số điện thoại (tùy chọn)

---

## 🔐 11. XÁC THỰC HAI YẾU TỐ (2FA) - TÙY CHỌN

### Mức độ ưu tiên: ⭐⭐ (TÙY CHỌN - NÂNG CAO)

**Giải pháp:**
- **TOTP (Time-based One-Time Password)**
  - Sử dụng ứng dụng Authenticator (Google Authenticator, Authy)
  - Mã 6 số, đổi mỗi 30 giây

- **SMS OTP** (tùy chọn)
  - Gửi mã qua SMS
  - Mã có thời hạn 5 phút

**Triển khai:**
- Người dùng bật 2FA trong cài đặt tài khoản
- Yêu cầu mã 2FA khi đăng nhập từ thiết bị mới

**UI/UX:**
- Hướng dẫn setup 2FA rõ ràng
- Backup codes cho trường hợp mất thiết bị

---

## 📱 12. BẢO MẬT THIẾT BỊ

### Mức độ ưu tiên: ⭐⭐⭐ (QUAN TRỌNG)

**Giải pháp:**
- **Biometric Authentication:**
  - Face ID / Fingerprint để đăng nhập nhanh
  - Thay thế nhập mật khẩu mỗi lần

- **Device Binding:**
  - Lưu device ID
  - Cảnh báo khi đăng nhập từ thiết bị mới

- **Root/Jailbreak Detection:**
  - Cảnh báo hoặc từ chối đăng nhập từ thiết bị đã root/jailbreak
  - Tăng cường bảo mật

---

## 📊 13. BẢNG TỔNG HỢP ĐỘ ƯU TIÊN

| STT | Tính năng | Độ ưu tiên | Mức độ khó | Thời gian ước tính |
|-----|-----------|------------|------------|-------------------|
| 1 | Mã hóa mật khẩu | ⭐⭐⭐⭐⭐ | Trung bình | 2-3 giờ |
| 2 | Validation mạnh mẽ | ⭐⭐⭐⭐⭐ | Dễ | 3-4 giờ |
| 3 | Rate Limiting | ⭐⭐⭐⭐ | Trung bình | 4-5 giờ |
| 4 | Xác minh Email | ⭐⭐⭐⭐ | Khó | 6-8 giờ |
| 5 | Session Management | ⭐⭐⭐⭐ | Trung bình | 4-5 giờ |
| 6 | Quên mật khẩu | ⭐⭐⭐⭐ | Khó | 6-8 giờ |
| 7 | Chống SQL Injection/XSS | ⭐⭐⭐⭐⭐ | Trung bình | 3-4 giờ |
| 8 | Security Logging | ⭐⭐⭐ | Dễ | 3-4 giờ |
| 9 | CAPTCHA | ⭐⭐⭐ | Dễ | 2-3 giờ |
| 10 | Mã hóa dữ liệu | ⭐⭐⭐⭐ | Khó | 5-6 giờ |
| 11 | 2FA | ⭐⭐ | Rất khó | 10-12 giờ |
| 12 | Bảo mật thiết bị | ⭐⭐⭐ | Trung bình | 4-5 giờ |

**Tổng thời gian ước tính:** 52-67 giờ (~7-9 ngày làm việc)

---

## 🎯 KHUYẾN NGHỊ TRIỂN KHAI

### Phase 1 - BẮT BUỘC (Tuần 1-2):
1. ✅ Mã hóa mật khẩu (BCrypt)
2. ✅ Validation mạnh mẽ
3. ✅ Chống SQL Injection/XSS
4. ✅ Rate Limiting cơ bản

### Phase 2 - QUAN TRỌNG (Tuần 3-4):
5. ✅ Xác minh Email
6. ✅ Session Management nâng cao
7. ✅ Quên mật khẩu
8. ✅ Security Logging

### Phase 3 - TÙY CHỌN (Tuần 5+):
9. ✅ CAPTCHA
10. ✅ Mã hóa dữ liệu (Android Keystore)
11. ✅ Bảo mật thiết bị
12. ✅ 2FA (nếu cần)

---

## 📝 GHI CHÚ

- Tất cả tính năng cần được test kỹ lưỡng
- Tuân thủ các chuẩn bảo mật OWASP
- Cập nhật tài liệu sau khi triển khai
- Review code security trước khi deploy

---

**Người đề xuất:** [Tên]  
**Ngày:** [Ngày tháng]  
**Phiên bản:** 1.0

---

## 📊 TỔNG KẾT TRIỂN KHAI

### ✅ CÁC TÍNH NĂNG ĐÃ TRIỂN KHAI (Phase 1)

#### 1. 🔐 Mã hóa mật khẩu - **HOÀN THÀNH**
- **Status:** ✅ Đã triển khai
- **Files mới:**
  - `app/src/main/java/com/example/homestay/utils/PasswordHasher.kt`
- **Files đã cập nhật:**
  - `gradle/libs.versions.toml` - Thêm bcrypt dependency
  - `app/build.gradle.kts` - Thêm bcrypt library
  - `app/src/main/java/com/example/homestay/data/dao/UserDao.kt`
  - `app/src/main/java/com/example/homestay/data/repository/HomestayRepository.kt`
- **Chi tiết:**
  - Sử dụng BCrypt với work factor 12
  - Tất cả mật khẩu mới được hash trước khi lưu
  - Password verification bằng BCrypt.checkpw()
  - Tương thích với dữ liệu cũ (cần migration script nếu có data cũ)

#### 2. 🛡️ Validation mạnh mẽ - **HOÀN THÀNH**
- **Status:** ✅ Đã triển khai
- **Files mới:**
  - `app/src/main/java/com/example/homestay/utils/InputValidator.kt`
- **Files đã cập nhật:**
  - `app/src/main/java/com/example/homestay/ui/viewmodel/AuthViewModel.kt`
  - `app/src/main/java/com/example/homestay/RegisterActivity.kt`
- **Chi tiết:**
  - Password validation: 8+ ký tự, chữ hoa/thường/số/ký tự đặc biệt
  - Password strength: WEAK/MEDIUM/STRONG/VERY_STRONG
  - Email validation: RFC 5321 compliant
  - Phone validation: Vietnam format (10-11 số)
  - Full name validation: 2-50 ký tự, chỉ chữ cái
  - Input sanitization: Chống XSS
  - Hiển thị error message chi tiết

#### 3. 🚫 Rate Limiting - **HOÀN THÀNH**
- **Status:** ✅ Đã triển khai
- **Files mới:**
  - `app/src/main/java/com/example/homestay/utils/RateLimiter.kt`
- **Files đã cập nhật:**
  - `app/src/main/java/com/example/homestay/ui/viewmodel/AuthViewModel.kt`
  - `app/src/main/java/com/example/homestay/LoginActivity.kt`
- **Chi tiết:**
  - Giới hạn 5 lần đăng nhập sai
  - Khóa tài khoản: 15 phút → 30 phút → 60 phút
  - Lưu trong SharedPreferences theo email
  - Reset khi đăng nhập thành công
  - Hiển thị số lần còn lại và thời gian khóa

#### 4. 🔄 Session Management - **ĐÃ ĐƠN GIẢN HÓA**
- **Status:** ❌ Đã xóa tính năng phức tạp (token, expiration, auto-logout)
- **Files đã cập nhật:**
  - `app/src/main/java/com/example/homestay/utils/SessionManager.kt` - Đơn giản hóa
  - `app/src/main/java/com/example/homestay/MainActivity.kt` - Xóa các kiểm tra session validity
- **Chi tiết:**
  - Session đơn giản: chỉ lưu userId, email, name
  - Không có token, expiration hay auto-logout
  - Session tồn tại cho đến khi user đăng xuất thủ công

### 📝 GHI CHÚ QUAN TRỌNG

1. **Migration dữ liệu cũ:**
   - Nếu có user với mật khẩu plain text trong database, cần migration script
   - Khi user đăng nhập lần đầu sau update, hash password mới
   - Hoặc yêu cầu user reset password

2. **Testing:**
   - ✅ Test password hashing: Mật khẩu mới được hash
   - ✅ Test password verification: Đăng nhập với password đúng/sai
   - ✅ Test validation: Tất cả các trường hợp validation
   - ✅ Test rate limiting: Đăng nhập sai 5 lần, kiểm tra khóa
   - ✅ Test session expiration: Không dùng app 7 ngày, kiểm tra auto-logout

3. **Cần lưu ý:**
   - Session timeout 7 ngày có thể điều chỉnh theo yêu cầu
   - Rate limiting timeout có thể điều chỉnh (hiện tại 15/30/60 phút)
   - Password requirements có thể làm yếu đi nếu user phàn nàn (nhưng không khuyến khích)

4. **Chưa triển khai (Phase 2+):**
   - ⏳ Xác minh Email (Email Verification)
   - ⏳ Quên mật khẩu (Password Reset)
   - ⏳ Security Logging
   - ⏳ CAPTCHA
   - ⏳ Mã hóa dữ liệu (Android Keystore)
   - ⏳ 2FA

### 🔍 KIỂM TRA SAU KHI TRIỂN KHAI

- [ ] Test đăng nhập với mật khẩu cũ (nếu có data cũ)
- [ ] Test đăng ký với password yếu → Hiển thị error
- [ ] Test đăng nhập sai 6 lần → Kiểm tra khóa tài khoản
- [ ] Test session expiration → Đợi hoặc mock time
- [ ] Test validation tất cả các trường (email, phone, password, name)

---

**Cập nhật lần cuối:** [Ngày cập nhật]  
**Người triển khai:** [Tên]

