@echo off
echo ============================================
echo  TESTING BACKEND LOCAL
echo ============================================
echo.

cd /d C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp\noti

echo [1/3] Checking Node.js installation...
node --version
if %errorlevel% neq 0 (
    echo ERROR: Node.js is not installed!
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

echo.
echo [2/3] Installing dependencies...
call npm install

echo.
echo [3/3] Starting local server...
echo.
echo ============================================
echo  Server will start at http://localhost:3000
echo ============================================
echo.
echo Press Ctrl+C to stop the server
echo.

node index.js

pause

