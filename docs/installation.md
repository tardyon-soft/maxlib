# Установка и состав модулей

## Требования

- Java 17
- Gradle или Maven

## Какой модуль подключать

- `max-client-core` - если нужен только typed client к MAX API.
- `max-dispatcher` - если нужен runtime-слой с `Dispatcher`, `Router`, filters, middleware, screens и polling/webhook ingestion API.
- `max-spring-boot-starter` - если приложение работает на Spring Boot.
- `max-testkit` - если нужно тестировать обработчики и побочные вызовы клиента.

## Gradle

Spring Boot:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-spring-boot-starter:<version>")
}
```

Vanilla Java:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-client-core:<version>")
    implementation("ru.tardyon.botframework:max-dispatcher:<version>")
}
```

Тесты:

```kotlin
dependencies {
    testImplementation("ru.tardyon.botframework:max-testkit:<version>")
}
```

## Maven

Spring Boot:

```xml
<dependency>
  <groupId>ru.tardyon.botframework</groupId>
  <artifactId>max-spring-boot-starter</artifactId>
  <version>${maxlib.version}</version>
</dependency>
```

Vanilla Java:

```xml
<dependency>
  <groupId>ru.tardyon.botframework</groupId>
  <artifactId>max-client-core</artifactId>
  <version>${maxlib.version}</version>
</dependency>
<dependency>
  <groupId>ru.tardyon.botframework</groupId>
  <artifactId>max-dispatcher</artifactId>
  <version>${maxlib.version}</version>
</dependency>
```

## Что дает Spring Boot starter

`max-spring-boot-starter` автоматически подключает:

- `MaxBotClient`
- `Dispatcher`
- `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`
- `FSMStorage` с типом `MEMORY` или `REDIS`
- `ScreenRegistry`, `SceneRegistry`, `SceneStorage`
- polling runtime при `max.bot.mode=POLLING`
- webhook runtime и HTTP endpoint при `max.bot.mode=WEBHOOK`

Если используются отдельные возможности Spring, нужны и стандартные зависимости Spring Boot:

- `spring-boot-starter-web` для webhook HTTP endpoint;
- `spring-boot-starter-validation` для валидации `@ConfigurationProperties`;
- `spring-boot-starter-data-redis` для `max.bot.storage.type=REDIS`.
