@echo off
echo Запуск фронтенда VPN Monitor System...
echo.
echo Если у вас установлен Node.js, используйте команду:
echo cd "src\main\vpn-ui" ^&^& npm run dev
echo.
echo Если Node.js не установлен, откройте в браузере:
echo http://localhost:8081
echo.
echo Или используйте Python HTTP сервер:
echo cd "src\main\vpn-ui" ^&^& python -m http.server 5173
echo.
pause