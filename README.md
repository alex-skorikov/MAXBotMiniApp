MAXBotMiniApp
=============
![Документация](./documentation.md)

--------
Описание
--------
MAXBotMiniApp — это Java/Spring Boot приложение, которое принимает события от платформы MAX через webhook,
обрабатывает входящие сообщения и отвечает пользователям через MaxApiClient.
Проект поддерживает деплой на Railway и полностью готов к продакшен‑использованию.

------------------------------------------------------------
СТЕК ТЕХНОЛОГИЙ
------------------------------------------------------------
- Java 17
- Spring Boot 3
- Maven Wrapper (mvnw)
- REST API
- MAX API
- Docker (Liberica JDK 17)
- Railway (автодеплой из GitHub)

------------------------------------------------------------
БЫСТРЫЙ СТАРТ
------------------------------------------------------------

1. Клонирование репозитория:

   git clone https://github.com/<your-username>/MAXBotMiniApp.git
   cd MAXBotMiniApp

------------------------------------------------------------
ЛОКАЛЬНЫЙ ЗАПУСК
------------------------------------------------------------

2. Сборка проекта:

   ./mvnw -q -DskipTests package

3. Запуск Spring Boot:

   ./mvnw spring-boot:run

Приложение будет доступно по адресу:
http://localhost:8080

Проверка состояния:
http://localhost:8080/actuator/health

------------------------------------------------------------
КОНФИГУРАЦИЯ (application.yml)
------------------------------------------------------------

server:
port: 8080

management:
endpoints:
web:
exposure:
include: health,info
endpoint:
health:
show-details: always

------------------------------------------------------------
НАСТРОЙКА ЧАТ-БОТА MAX
------------------------------------------------------------

1. Создать бота в MAX:
   MAX → Чат‑боты → Создать бота

2. Получить токен бота:
   Bot Token: xxxxxxx

3. Добавить токен в Railway → Variables:

   MAX_API_TOKEN=xxxxx

4. Указать webhook в настройках MAX:

   https://maxbotminiapp-production.up.railway.app/webhook

5. Проверить работу:
   Написать боту → смотреть логи Railway:
   Railway → MAXBotMiniApp → Logs

------------------------------------------------------------
DOCKERFILE
------------------------------------------------------------

FROM bellsoft/liberica-openjdk-debian:17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

FROM bellsoft/liberica-openjdk-debian:17
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

------------------------------------------------------------
ДЕПЛОЙ НА RAILWAY
------------------------------------------------------------

1. Подключить GitHub репозиторий:
   Railway → New Project → Deploy from GitHub

2. Railway автоматически найдёт Dockerfile

3. Переменные окружения:

   MAX_API_TOKEN=xxxxx
   JAVA_OPTS=-Xmx512m

4. Деплой запускается автоматически при каждом push

5. Проверить состояние:

   https://maxbotminiapp-production.up.railway.app/actuator/health

------------------------------------------------------------
ТЕСТИРОВАНИЕ WEBHOOK
------------------------------------------------------------

curl -X POST https://maxbotminiapp-production.up.railway.app/webhook \
-H "Content-Type: application/json" \
-d '{"message":{"chat_id":"123","text":"ping"}}'

Ответ:
{"ok": true}

------------------------------------------------------------
СТРУКТУРА ПРОЕКТА
------------------------------------------------------------

MAXBotMiniApp/
├── src/main/java/org/maxbot/miniapp/
│    ├── controller/MaxBotController.java
│    ├── client/MaxApiClient.java
│    └── service/PatentSearchService.java
├── src/main/resources/application.yml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/
├── Dockerfile
├── pom.xml
└── README.txt

