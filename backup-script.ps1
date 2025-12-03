# Script Backup Tự động cho Homestay2
# Cách sử dụng: Nhấp phải vào file này -> Run with PowerShell

# Lấy ngày giờ hiện tại
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm"
$backupName = "homestay2_backup_$timestamp.zip"

# Đường dẫn thư mục dự án
$sourceDir = "C:\Users\ADMIN\AndroidStudioProjects\homestay2"

# Đường dẫn lưu backup (thay đổi theo ý bạn)
$backupDir = "D:\Backups"

# Tạo thư mục backup nếu chưa có
if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir | Out-Null
    Write-Host "✅ Đã tạo thư mục backup: $backupDir" -ForegroundColor Green
}

# Đường dẫn file backup đầy đủ
$backupPath = Join-Path $backupDir $backupName

Write-Host "🔄 Đang tạo backup..." -ForegroundColor Yellow
Write-Host "📁 Nguồn: $sourceDir" -ForegroundColor Cyan
Write-Host "💾 Đích: $backupPath" -ForegroundColor Cyan

# Tạo file ZIP (loại trừ thư mục .git, node_modules, build)
$excludeDirs = @(".git", "node_modules", "build", ".gradle", ".idea")

# Tạo backup
try {
    # Sử dụng Compress-Archive để tạo ZIP
    $filesToBackup = Get-ChildItem -Path $sourceDir -Recurse | 
        Where-Object { 
            $exclude = $false
            foreach ($dir in $excludeDirs) {
                if ($_.FullName -like "*\$dir\*" -or $_.Name -eq $dir) {
                    $exclude = $true
                    break
                }
            }
            -not $exclude
        }
    
    # Tạo file ZIP tạm
    $tempZip = "$env:TEMP\$backupName"
    Compress-Archive -Path $sourceDir\* -DestinationPath $tempZip -Force
    
    # Di chuyển đến vị trí cuối cùng
    Move-Item -Path $tempZip -Destination $backupPath -Force
    
    $fileSize = (Get-Item $backupPath).Length / 1MB
    Write-Host "✅ Backup thành công!" -ForegroundColor Green
    Write-Host "📦 File: $backupName" -ForegroundColor Green
    Write-Host "💾 Kích thước: $([math]::Round($fileSize, 2)) MB" -ForegroundColor Green
    Write-Host "📍 Vị trí: $backupPath" -ForegroundColor Green
    
    # Mở thư mục chứa backup
    Write-Host "`n🔍 Mở thư mục backup..." -ForegroundColor Yellow
    Start-Process explorer.exe -ArgumentList $backupDir
    
} catch {
    Write-Host "❌ Lỗi khi tạo backup: $_" -ForegroundColor Red
}

Write-Host "`n✨ Hoàn tất! Nhấn phím bất kỳ để đóng..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
