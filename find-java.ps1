# PowerShell script to find Java installations on Windows
Write-Host "Searching for Java installations..." -ForegroundColor Cyan
Write-Host ""

# Check common Java installation paths
$javaPaths = @(
    "C:\Program Files\Java",
    "C:\Program Files (x86)\Java",
    "$env:ProgramFiles\Android\Android Studio\jbr",
    "$env:LOCALAPPDATA\Programs\Android\Android Studio\jbr",
    "$env:ProgramFiles\JetBrains\Android Studio\jbr",
    "$env:LOCALAPPDATA\Programs\Android",
    "$env:LOCALAPPDATA\Programs\Java",
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Microsoft",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Microsoft"
)

$foundJavas = @()

foreach ($path in $javaPaths) {
    if (Test-Path $path) {
        Write-Host "Checking: $path" -ForegroundColor Yellow
        $dirs = Get-ChildItem -Path $path -Directory -ErrorAction SilentlyContinue
        
        foreach ($dir in $dirs) {
            $javaExe = Join-Path $dir.FullName "bin\java.exe"
            if (Test-Path $javaExe) {
                # Try to get version
                try {
                    $versionOutput = & $javaExe -version 2>&1
                    $versionLine = $versionOutput | Select-Object -First 1
                    Write-Host "  ✓ Found: $($dir.FullName)" -ForegroundColor Green
                    Write-Host "    Version: $versionLine" -ForegroundColor Gray
                    $foundJavas += $dir.FullName
                } catch {
                    Write-Host "  ✓ Found: $($dir.FullName) (version check failed)" -ForegroundColor Green
                    $foundJavas += $dir.FullName
                }
            }
        }
    }
}

# Check JAVA_HOME
Write-Host ""
Write-Host "Environment Variables:" -ForegroundColor Cyan
if ($env:JAVA_HOME) {
    Write-Host "  JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
    if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
        Write-Host "    ✓ Valid Java installation" -ForegroundColor Green
    } else {
        Write-Host "    ✗ Invalid - java.exe not found" -ForegroundColor Red
    }
} else {
    Write-Host "  JAVA_HOME: Not set" -ForegroundColor Yellow
}

# Check PATH
Write-Host ""
$javaInPath = Get-Command java -ErrorAction SilentlyContinue
if ($javaInPath) {
    Write-Host "  Java in PATH: $($javaInPath.Source)" -ForegroundColor Green
} else {
    Write-Host "  Java in PATH: Not found" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
if ($foundJavas.Count -gt 0) {
    Write-Host "Found $($foundJavas.Count) Java installation(s):" -ForegroundColor Green
    foreach ($java in $foundJavas) {
        Write-Host "  - $java" -ForegroundColor White
    }
    Write-Host ""
    Write-Host "To use one of these, update .vscode/settings.json:" -ForegroundColor Yellow
    Write-Host '  "java.jdt.ls.java.home": "' -NoNewline -ForegroundColor Gray
    Write-Host "$($foundJavas[0])" -NoNewline -ForegroundColor White
    Write-Host '"' -ForegroundColor Gray
} else {
    Write-Host "No Java installations found." -ForegroundColor Red
    Write-Host "Please install JDK 17 or higher. See JAVA_SETUP_GUIDE.md for instructions." -ForegroundColor Yellow
}

