# 📚 HƯỚNG DẪN SAO LƯU DỰ ÁN HOMESTAY2

## 🎯 Tổng quan
Tài liệu này hướng dẫn chi tiết các cách sao lưu dự án Homestay2 để bảo vệ code của bạn.

---

## ✅ PHƯƠNG PHÁP 1: Sao lưu lên GitHub (KHUYÊN DÙNG)

### Khi nào cần làm?
- ✅ Sau khi code xong trong ngày
- ✅ Trước khi tắt máy
- ✅ Trước khi thay đổi lớn
- ✅ Trước khi nộp đồ án

### Các bước thực hiện:

#### Bước 1: Mở Terminal
- Trong VS Code: Nhấn `Ctrl + ~` hoặc `Terminal > New Terminal`

#### Bước 2: Chạy 3 lệnh sau:

```bash
# 1. Thêm tất cả file đã thay đổi
git add .

# 2. Tạo commit với mô tả (thay đổi message cho phù hợp)
git commit -m "Mô tả những gì bạn đã sửa"

# 3. Đẩy lên GitHub
git push
```

#### Ví dụ commit message:
```bash
git commit -m "feat: Thêm chức năng tìm kiếm homestay"
git commit -m "fix: Sửa lỗi đăng nhập"
git commit -m "docs: Cập nhật tài liệu API"
git commit -m "refactor: Tối ưu code LoginActivity"
```

### Kiểm tra đã backup thành công:
1. Truy cập: https://github.com/baviet0501-cyber/homestay2
2. Xem commit mới nhất có xuất hiện không

---

## 💾 PHƯƠNG PHÁP 2: Download ZIP từ GitHub

### Bước 1: Truy cập GitHub
Mở trình duyệt và vào: https://github.com/baviet0501-cyber/homestay2

### Bước 2: Download
1. Nhấn nút **Code** (màu xanh lá)
2. Chọn **Download ZIP**
3. Lưu file vào:
   - `D:\Backups\`
   - Google Drive
   - Ổ cứng ngoài

### Bước 3: Đổi tên file
Đổi tên thành: `homestay2_backup_DD-MM-YYYY.zip`

Ví dụ: `homestay2_backup_03-12-2025.zip`

---

## 🤖 PHƯƠNG PHÁP 3: Sử dụng Script Tự động (ĐƠN GIẢN NHẤT)

### Cách 1: Chạy bằng cách nhấp đúp

1. Tìm file `backup-script.ps1` trong thư mục dự án
2. **Nhấp phải** vào file
3. Chọn **"Run with PowerShell"**
4. Đợi script chạy xong
5. File backup sẽ được tạo tại `D:\Backups\`

### Cách 2: Chạy từ Terminal

```powershell
# Di chuyển đến thư mục dự án
cd C:\Users\ADMIN\AndroidStudioProjects\homestay2

# Chạy script
.\backup-script.ps1
```

### Lưu ý:
- Script sẽ tự động loại bỏ các thư mục không cần thiết (.git, node_modules, build)
- File backup có tên dạng: `homestay2_backup_2025-12-03_14-22.zip`
- Mặc định lưu tại `D:\Backups\` (có thể thay đổi trong script)

### Thay đổi vị trí lưu backup:
Mở file `backup-script.ps1` và sửa dòng:
```powershell
$backupDir = "D:\Backups"  # Thay đổi đường dẫn này
```

---

## 🖥️ PHƯƠNG PHÁP 4: Clone sang vị trí khác

### Sao lưu sang ổ đĩa khác:

```bash
# Bước 1: Mở PowerShell hoặc Terminal

# Bước 2: Di chuyển đến ổ đĩa muốn backup
cd D:\

# Bước 3: Clone repository
git clone https://github.com/baviet0501-cyber/homestay2.git homestay2_backup
```

### Kết quả:
- Bản làm việc: `C:\Users\ADMIN\AndroidStudioProjects\homestay2`
- Bản backup: `D:\homestay2_backup`

---

## 🔄 PHƯƠNG PHÁP 5: Tạo Branch Backup

### Khi nào dùng?
- Trước khi thay đổi lớn
- Trước khi refactor code
- Trước khi merge code từ người khác

### Các bước:

```bash
# Tạo branch backup với tên có ngày tháng
git branch backup-2025-12-03

# Đẩy branch backup lên GitHub
git push origin backup-2025-12-03
```

### Khôi phục từ branch backup:
```bash
# Chuyển về branch backup
git checkout backup-2025-12-03
```

---

## 📅 LỊCH TRÌNH SAO LƯU KHUYẾN NGHỊ

### Hàng ngày:
- ✅ Push lên GitHub sau khi code xong (Phương pháp 1)

### Hàng tuần:
- ✅ Download ZIP từ GitHub (Phương pháp 2)
- ✅ Chạy script backup tự động (Phương pháp 3)

### Trước các sự kiện quan trọng:
- ✅ Tạo branch backup (Phương pháp 5)
- ✅ Clone sang ổ đĩa khác (Phương pháp 4)
- ✅ Lưu vào Google Drive

---

## 🛡️ CHIẾN LƯỢC 3-2-1

### Nguyên tắc vàng:
- **3** bản sao
- **2** phương tiện khác nhau
- **1** bản ở nơi khác (cloud)

### Áp dụng cho Homestay2:
1. ✅ **GitHub** (Cloud)
2. ✅ **Máy tính** (C:\Users\ADMIN\AndroidStudioProjects\homestay2)
3. ✅ **Ổ cứng ngoài hoặc D:\Backups**

---

## ❓ KHẮC PHỤC SỰ CỐ

### Mất code trên máy local:
```bash
# Clone lại từ GitHub
git clone https://github.com/baviet0501-cyber/homestay2.git
```

### Sửa nhầm và muốn quay lại:
```bash
# Xem lịch sử commit
git log

# Quay lại commit trước đó
git reset --hard <commit-id>
```

### Xóa nhầm file:
```bash
# Khôi phục file từ commit cuối
git checkout HEAD -- <tên-file>
```

---

## 📞 LIÊN HỆ

Nếu cần hỗ trợ thêm, hãy tham khảo:
- GitHub Docs: https://docs.github.com
- Git Documentation: https://git-scm.com/doc

---

**Lưu ý:** Hãy backup thường xuyên để bảo vệ công sức của bạn! 💪
