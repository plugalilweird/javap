# 🔒 VPN Monitor Scanner

Система мониторинга и сканирования VPN узлов с веб-интерфейсом.

## 🚀 Быстрый запуск

### Вариант 1: Автоматический запуск (Windows)
1. Дважды кликните на `start-system.bat`
2. Дождитесь запуска бэкенда (порт 8082)
3. Автоматически откроется тестовая страница

### Вариант 2: Ручной запуск

#### 1. Запуск бэкенда
```bash
# В корневой директории проекта
.\gradlew bootRun
```
Бэкенд будет доступен на http://localhost:8082

#### 2. Тестирование API
Откройте `test-api.html` в браузере для тестирования API

## 🌐 Доступные endpoints

- **API**: http://localhost:8082/api/vpn/nodes
- **Health Check**: http://localhost:8082/actuator/health
- **H2 Console**: http://localhost:8082/h2-console
- **Test Page**: test-api.html (откройте в браузере)

## 🛠 Технологии

### Backend
- **Spring Boot 3.1.2** - основной фреймворк
- **Spring Security** - безопасность
- **Spring Data JPA** - работа с базой данных
- **H2 Database** - in-memory база данных для разработки
- **PostgreSQL** - production база данных
- **Spring Actuator** - мониторинг приложения

### Frontend
- **HTML/CSS/JavaScript** - тестовая страница для проверки API

## 📁 Структура проекта

```
freevpnscanner/
├── src/main/java/com/example/vpnmonitor/
│   ├── VpnMonitorApplication.java     # Главный класс приложения
│   ├── config/
│   │   └── SecurityConfig.java        # Конфигурация безопасности
│   ├── controller/
│   │   └── VPNController.java         # REST API контроллер
│   ├── model/
│   │   └── VPNNode.java               # Модель VPN узла
│   ├── repository/
│   │   └── VPNNodeRepository.java     # Репозиторий для работы с БД
│   └── service/
│       └── VPNScannerService.java     # Сервис сканирования
├── src/main/resources/
│   └── application.properties         # Конфигурация приложения
├── src/main/vpn-ui/                   # React фронтенд
├── test-api.html                      # Тестовая HTML страница
├── start-system.bat                   # Скрипт запуска для Windows
└── build.gradle                       # Конфигурация сборки
```

## 🔧 Конфигурация

### База данных
По умолчанию используется H2 in-memory база данных. Для переключения на PostgreSQL:

1. Раскомментируйте настройки PostgreSQL в `application.properties`
2. Закомментируйте настройки H2
3. Убедитесь, что PostgreSQL запущен

### Порт
Бэкенд по умолчанию работает на порту 8082. Измените `server.port` в `application.properties` при необходимости.

## 🧪 Тестирование

### API Endpoints
- `GET /api/vpn/nodes` - получить все VPN узлы
- `POST /api/vpn/scan` - запустить сканирование
- `POST /api/vpn/connect/{id}` - подключиться к узлу
- `POST /api/vpn/disconnect` - отключиться

### Тестовая страница
Откройте `test-api.html` в браузере для интерактивного тестирования API.

## 🚨 Устранение неполадок

### Порт занят
Если порт 8082 занят, измените `server.port` в `application.properties`

### CORS ошибки
Проверьте настройки CORS в `VpnMonitorApplication.java`

### База данных
- H2 консоль доступна по адресу http://localhost:8082/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (пустое)

## 📝 Логи

Логи приложения выводятся в консоль. Для включения debug режима добавьте в `application.properties`:
```properties
logging.level.com.example.vpnmonitor=DEBUG
```

## 🔒 Безопасность

- Spring Security включен по умолчанию
- API endpoints `/api/**` доступны без аутентификации
- H2 консоль и Actuator endpoints также доступны
- Для production настройте аутентификацию и авторизацию

## 📊 Мониторинг

Spring Actuator предоставляет следующие endpoints:
- `/actuator/health` - состояние приложения
- `/actuator/info` - информация о приложении
- `/actuator/metrics` - метрики

## 🤝 Разработка

### Добавление новых endpoints
1. Создайте метод в `VPNController`
2. Добавьте маппинг с аннотацией `@RequestMapping`
3. Обновите CORS конфигурацию при необходимости

### Изменение модели данных
1. Обновите `VPNNode.java`
2. Измените `spring.jpa.hibernate.ddl-auto` на `update` или `create-drop`
3. Перезапустите приложение

## 📄 Лицензия

Этот проект предназначен для образовательных целей.
