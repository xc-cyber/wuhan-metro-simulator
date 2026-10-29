@echo off
setlocal
cd /d "%~dp0"

echo ============================================
echo    Wuhan Metro Simulator - Compile and Run
echo ============================================
echo.

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac not found. Please install JDK 1.8+ and add bin to PATH.
    pause
    exit /b 1
)

echo [1/2] Compiling...
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\metro\*.java src\metro\exception\*.java
if errorlevel 1 (
    echo.
    echo [ERROR] Compile failed. Please check the messages above.
    pause
    exit /b 1
)

echo [2/2] Running...
echo.
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp bin metro.Test

echo.
echo ============================================
echo   Finished. Press any key to close.
echo ============================================
pause >nul