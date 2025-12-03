# Java Setup Guide - Fix JDK 17+ Requirement

## Problem
You're seeing the error: "JDK 17 or higher is required. Please set a valid Java home path to 'java.jdt.ls.java.home' setting or JAVA_HOME environment variable."

The error dialog also shows that `jli.dll` is missing, indicating a corrupted Java installation.

## Solution Options

### Option 1: Install JDK 17 or Higher (Recommended)

1. **Download JDK 17 or higher:**
   - **Eclipse Temurin (Recommended):** https://adoptium.net/
   - **Oracle JDK:** https://www.oracle.com/java/technologies/downloads/
   - **Microsoft Build of OpenJDK:** https://www.microsoft.com/openjdk

2. **Install the JDK:**
   - Run the installer
   - Note the installation path (usually `C:\Program Files\Java\jdk-17` or similar)

3. **Configure VS Code/Cursor:**
   - Open `.vscode/settings.json` in this project
   - Set `java.jdt.ls.java.home` to your JDK path, for example:
     ```json
     "java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-17"
     ```

4. **Set JAVA_HOME Environment Variable (Optional but recommended):**
   - Open System Properties → Environment Variables
   - Add new System Variable:
     - Name: `JAVA_HOME`
     - Value: `C:\Program Files\Java\jdk-17` (your JDK path)
   - Add to PATH: `%JAVA_HOME%\bin`

### Option 2: Use Android Studio's Bundled JDK

If you have Android Studio installed, it comes with a bundled JDK (JBR - JetBrains Runtime).

1. **Find Android Studio's JDK:**
   - Common locations:
     - `C:\Program Files\Android\Android Studio\jbr`
     - `C:\Users\YOUR_USERNAME\AppData\Local\Android\AndroidStudio\jbr`
     - `C:\Program Files\JetBrains\Android Studio\jbr`

2. **Verify it exists:**
   - Navigate to the folder
   - Check that `bin\java.exe` exists

3. **Configure VS Code/Cursor:**
   - Open `.vscode/settings.json` in this project
   - Set `java.jdt.ls.java.home` to the JBR path, for example:
     ```json
     "java.jdt.ls.java.home": "C:\\Program Files\\Android\\Android Studio\\jbr"
     ```

### Option 3: Reinstall Java (If Current Installation is Corrupted)

If you have a corrupted Java installation:

1. **Uninstall existing Java:**
   - Go to Control Panel → Programs → Uninstall
   - Remove all Java/JDK installations

2. **Clean up:**
   - Delete any remaining Java folders
   - Remove Java from PATH environment variable

3. **Install fresh JDK 17+** (follow Option 1)

## After Configuration

1. **Reload VS Code/Cursor:**
   - Press `Ctrl+Shift+P`
   - Type "Reload Window" and select it

2. **Verify Java is working:**
   - Open a terminal in VS Code/Cursor
   - Run: `java -version`
   - You should see Java version 17 or higher

3. **Restart Java Language Server:**
   - Press `Ctrl+Shift+P`
   - Type "Java: Clean Java Language Server Workspace"
   - Select it and restart

## Quick Check Commands

Run these in PowerShell to find Java installations:

```powershell
# Check if Java is in PATH
where java

# Check JAVA_HOME
$env:JAVA_HOME

# Find Java installations
Get-ChildItem -Path "C:\Program Files\Java" -Directory
Get-ChildItem -Path "C:\Program Files\Android\Android Studio\jbr" -Directory
```

## Project Configuration

This project is configured for:
- **JDK 21** (as seen in `.idea/misc.xml`)
- **Java 11** compatibility (as seen in `app/build.gradle.kts`)

The Java Language Server requires JDK 17+ to run, but your project code can target Java 11.

