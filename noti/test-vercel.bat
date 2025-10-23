@echo off
echo ============================================
echo  TESTING VERCEL DEPLOYMENT
echo ============================================
echo.

set URL=https://noti-colfwtzou-anh-nguyens-projects-df238e73.vercel.app

echo Testing current URL: %URL%
echo.

echo [1/2] Testing Health Endpoint...
curl -s %URL%/api/health
echo.
echo.

echo [2/2] Testing Root Endpoint...
curl -s %URL%/
echo.
echo.

echo ============================================
echo.
echo If you see 404 errors above, you need to:
echo 1. Get your new Vercel URL from dashboard
echo 2. Update BASE_URL in NotificationAPI.java
echo.
echo To get new URL:
echo - Go to https://vercel.com/dashboard
echo - Find your 'noti' project
echo - Copy the production URL
echo.
pause

