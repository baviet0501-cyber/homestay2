# Homestay Backend Server

Backend server cho ứng dụng Homestay Android, chạy trên localhost.

## Yêu Cầu

- Node.js (v16 trở lên)
- npm hoặc yarn

## Cài Đặt

1. Cài đặt dependencies:
```bash
npm install
```

## Cấu Hình

1. Tìm IP address của máy tính:
   - Windows: `ipconfig`
   - Linux/Mac: `ifconfig`
   - Tìm IPv4 Address (ví dụ: `192.168.1.100`)

2. Cập nhật `ApiConfig.kt` trong Android app:
   ```kotlin
   const val BASE_URL = "http://192.168.1.100:3000/api/"
   ```

3. Mở Firewall cho port 3000 (xem hướng dẫn trong `HUONG_DAN_LOCALHOST_SERVER.md`)

## Chạy Server

```bash
npm start
```

Hoặc với auto-reload (development):
```bash
npm run dev
```

Server sẽ chạy tại `http://localhost:3000`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập

### Users
- `GET /api/users/:id` - Lấy thông tin user
- `PUT /api/users/:id` - Cập nhật thông tin user

### Rooms
- `GET /api/rooms` - Lấy tất cả phòng
- `GET /api/rooms/:id` - Lấy thông tin phòng
- `GET /api/rooms/search?q=query` - Tìm kiếm phòng

### Bookings
- `GET /api/bookings?userId=:userId` - Lấy bookings của user
- `POST /api/bookings` - Tạo booking mới
- `PUT /api/bookings/:id` - Cập nhật booking

### Favorites
- `GET /api/favorites?userId=:userId` - Lấy favorites của user
- `POST /api/favorites` - Thêm favorite
- `DELETE /api/favorites/:id` - Xóa favorite

## Database

Database SQLite được lưu tại `./database/homestay.db`

Database sẽ được tạo tự động khi server chạy lần đầu.

## Test API

Mở browser và truy cập:
```
http://localhost:3000/api/rooms
```

Hoặc dùng curl:
```bash
curl http://localhost:3000/api/rooms
```

## Troubleshooting

### Lỗi: "Cannot find module"
```bash
npm install
```

### Lỗi: "Port already in use"
Thay đổi PORT trong file `.env` hoặc kill process đang dùng port 3000

### Android app không kết nối được
1. Kiểm tra server đã chạy
2. Kiểm tra IP address đúng
3. Kiểm tra firewall
4. Đảm bảo Android và máy tính cùng mạng WiFi


