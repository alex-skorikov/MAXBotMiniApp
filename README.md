MAXBotMiniApp
=============
### Статус проекта
![Coverage](jacoco-coverage-badge.svg) &nbsp; ![Checkstyle](checkstyle-badge.svg)

=============

### [Схема работы бота](documentation/bot.png)
### [Схема работы мини приложения](documentation/web-app.png)
### [Документация-1](documentation/documentation.md)
### [Ссылка на документацию-2](https://docs.google.com/document/d/1KE00UTb-4Y83kBFTeeda5ajMQYxt3pBqNL-ga1uO8ew/edit?tab=t.0)

---

### Описание
**MAXBotMiniApp** — это реактивное Java-приложение на базе **Spring WebFlux**, 
реализующее архитектуру чат-бота со встроенным Mini App для мессенджера MAX. 
Приложение интегрировано с поисковым API Роспатента, 
использует конечный автомат **Spring State Machine** для ведения пошаговых диалогов и кэширует контекст пользователей в **Redis**.

---

### Стек технологий
- **Core:** Java 17, Spring Boot 3.2.4
- **Реактивный движок:** Spring WebFlux (Project Reactor), Netty
- **Управление состоянием:** Spring State Machine 3.2.0
- **База данных и кэш:** Spring Data Redis
- **Качество кода и линтинг:** Apache Maven Checkstyle Plugin 3.3.1
- **Тестирование и покрытие:** JUnit 5, Mockito, StepVerifier, JaCoCo (0.8.11)
- **Безопасность окружения:** Fail2ban, Nginx (Reverse Proxy)

---

### Переменные окружения (application.properties)

Для успешного запуска приложения (локально или на серверах Railway/Docker) 
необходимо настроить следующие переменные окружения:

| Переменная | Описание | Значение по умолчанию / Пример |
| :--- | :--- | :--- |
| `PORT` | Порт, на котором слушает веб-сервер | `8080` |
| `MAX_TOKEN` | Секретный токен вашего бота в мессенджере MAX | `your_max_bot_token` |
| `MAX_BOT_NAME` | Юзернейм бота в MAX (используется для Mini App диплинков) | `my_patent_bot` |
| `ROSPATENT_TOKEN` | API-ключ для доступа к поисковой платформе Роспатента | `your_rospatent_token` |
| `REDIS_HOST` | Хост сервера базы данных Redis | `localhost` |
| `REDIS_PORT` | Порт сервера базы данных Redis | `6379` |

---

### Быстрый старт и локальный запуск

### 1. Клонирование репозитория
```bash
git clone https://github.com<your-username>/MAXBotMiniApp.git
cd MAXBotMiniApp
```

### 2. Сборка проекта
Сборка и упаковка JAR-файла (с запуском тестов и валидацией Checkstyle):
```bash
./mvnw clean package
```
*Для быстрой сборки без тестов:* `./mvnw clean package -DskipTests`

### 3. Запуск приложения
```bash
./mvnw spring-boot:run
```
После старта приложение доступно по адресу: `http://localhost:8080`

### 4. Мониторинг состояния (Actuator)
- Проверить работоспособность: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

### Структура проекта

```text
MAXBotMiniApp/
├── config/
│    └── checkstyle.xml         # Правила линтинга кода
├── src/main/java/org/maxbot/miniapp/
│    ├── MaxBotMiniAppApplication.java
│    ├── client/                # Реактивные HTTP-клиенты (MaxApiClient, Роспатент)
│    ├── controller/            # REST-эндпоинты (WebBotController, WebAppController)
│    ├── core/                  # Бизнес-модели данных (UserContext, BotEvent)
│    ├── dto/                   # Модели обмена данными (Запросы/Ответы Роспатента и MAX)
│    ├── handlers/              # Обработчики шагов стейт-машины (DateFilterHandler и др.)
│    ├── repository/            # Работа со слоем данных Redis
│    ├── statemachine/          # Конфигурация переходов и состояний Spring State Machine
│    └── util/                  # Гарды и утилиты валидации (ValidDateGuard)
├── src/main/resources/
│    ├── application.properties # Конфигурация Spring Boot приложений
│    └── logback.xml            # Настройки ротации логирования и фильтрации
├── Dockerfile
├── lombok.config               # Исключение генераций Lombok из отчетов JaCoCo
└── pom.xml                     # Спецификация зависимостей Maven
```

---

### Тестирование и качество кода

### Проверка покрытия тестами (JaCoCo)
Инструмент JaCoCo настроен на автоматическое исключение DTO, автоконфигураций и сгенерированного кода Lombok (через `lombok.addLombokGeneratedAnnotation=true`). Чтобы запустить тесты и сформировать HTML-отчет, выполните:
```bash
./mvnw clean test
```
Отчет будет сгенерирован по пути: `target/site/jacoco/index.html`.

### Проверка Checkstyle
Анализатор кода проверяет исходный код и тест-директории на фазе `validate`. Сборка упадет, если в коде присутствуют неиспользуемые импорты или нарушено форматирование.
```bash
./mvnw checkstyle:check
```
