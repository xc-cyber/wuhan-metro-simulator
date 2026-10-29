@echo off
setlocal
cd /d "%~dp0"

echo ============================================
echo    Wuhan Metro - GUI Mode
echo ============================================
echo.

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac not found.
    pause
    exit /b 1
)

if not exist bin mkdir bin
echo Compiling...
javac -encoding UTF-8 -d bin src\metro\*.java src\metro\exception\*.java
if errorlevel 1 (
    echo.
    echo [ERROR] Compile failed.
    pause
    exit /b 1
)

echo Starting GUI...
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp bin metro.MetroUI
if errorlevel 1 pause
