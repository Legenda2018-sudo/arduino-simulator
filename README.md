# Симулятор Arduino

Настольное приложение на JavaFX для сборки схем на макетной плате, симуляции цепей и сохранения проектов (локально и в Firebase).

## Стек технологий

- Java 17
- JavaFX 17
- Maven
- Firebase (Auth + Realtime Database)
- OkHttp, Gson
- JUnit 5

## Установка и запуск

1. Установите **JDK 17** (Eclipse Temurin или аналог).
2. Клонируйте репозиторий.
3. Создайте `src/main/resources/config.properties`:

```properties
firebase.url=https://YOUR-PROJECT-default-rtdb.firebaseio.com
firebase.apiKey=YOUR_WEB_API_KEY
```

4. Запуск из IDE: класс `org.example.arduino.HelloApplication`.

Или из командной строки:

```bat
mvnw.cmd javafx:run
```

## Использование

1. Выберите компонент слева (LED, кнопка, резистор, Arduino, таймер).
2. Кликните по точке на макетной плате для размещения.
3. Режим **«Соединить»** — провода между компонентами и шинами **+5V** / **GND**.
4. **«Симуляция»** — проверка цепи (замкнутый контур, ток, перегрузка LED).
5. **Файлы** — сохранение/загрузка JSON, экспорт PNG, облако Firebase (после входа).

## Структура проекта

```
src/main/java/org/example/arduino/
  ArduinoController.java   — UI и симуляция
  model/                   — компоненты, провода, плата
  service/                 — Firebase
  util/                    — CircuitPhysics, PowerRailSimulator
src/main/resources/
  arduino-view.fxml
  styles.css
src/test/java/             — JUnit-тесты
```

## Тесты

```bat
mvnw.cmd test
```
