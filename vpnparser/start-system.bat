@echo off
echo ========================================
echo    VPN Monitor System Starter
echo ========================================
echo.

echo Starting Backend Server on port 8082...
start "Backend Server" cmd /k "cd /d %~dp0 && .\gradlew bootRun"

echo.
echo Waiting for backend to start...
timeout /t 10 /nobreak >nul

echo.
echo Backend should be running on http://localhost:8082
echo.
echo Available endpoints:
echo - API: http://localhost:8082/api/vpn/nodes
echo - Health: http://localhost:8082/actuator/health
echo - H2 Console: http://localhost:8082/h2-console
echo.
echo To test the API, open test-api.html in your browser
echo.
echo Press any key to open test page...
pause >nul

echo Opening test page...
start test-api.html

echo.
echo System started successfully!
echo.
echo Press any key to exit...
pause >nul