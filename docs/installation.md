# Установка и состав модулей

## Требования

- Java 17
- Gradle или Maven

## Какой модуль подключать

- `max-client-core` - если нужен только typed client к MAX API.
- `max-dispatcher` - если нужен runtime-слой с `Dispatcher`, `Router`, filters, middleware, screens и polling/webhook ingestion API.
- `max-spring-boot-starter` - если приложение работает на Spring Boot.
- `max-micronaut-starter` - если приложение работает на Micronaut.
- `max-quarkus-starter` - если приложение работает на Quarkus.
- `max-testkit` - если нужно тестировать обработчики и побочные вызовы клиента.

## Gradle

Spring Boot:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-spring-boot-starter:<version>")
}
```

Micronaut:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-micronaut-starter:<version>")
}
```

Quarkus:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-quarkus-starter:<version>")
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

Micronaut:

```xml
<dependency>
  <groupId>ru.tardyon.botframework</groupId>
  <artifactId>max-micronaut-starter</artifactId>
  <version>${maxlib.version}</version>
</dependency>
```

Quarkus:

```xml
<dependency>
  <groupId>ru.tardyon.botframework</groupId>
  <artifactId>max-quarkus-starter</artifactId>
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

## Что дает Micronaut starter

`max-micronaut-starter` автоматически подключает:

- `MaxBotClient`
- `Dispatcher`
- `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`
- `FSMStorage` с типом `MEMORY` или `REDIS`
- `ScreenRegistry`, `SceneRegistry`, `SceneStorage`
- polling runtime при `max.bot.mode=POLLING`
- webhook runtime и HTTP endpoint при `max.bot.mode=WEBHOOK`

Если используются отдельные возможности Micronaut, нужны и стандартные зависимости Micronaut:

- `io.micronaut:micronaut-http-server-netty` для webhook HTTP endpoint;
- `io.micronaut.validation:micronaut-validation` для валидации configuration properties;
- `io.micronaut.redis:micronaut-redis-lettuce` для `max.bot.storage.type=REDIS`.

## Что дает Quarkus starter

`max-quarkus-starter` автоматически подключает:

- `MaxBotClient`
- `Dispatcher`
- `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`
- `FSMStorage` с типом `MEMORY` или `REDIS`
- `ScreenRegistry`, `SceneRegistry`, `SceneStorage`
- polling runtime при `max.bot.mode=POLLING`
- webhook runtime и HTTP endpoint при `max.bot.mode=WEBHOOK`

Если используются отдельные возможности Quarkus, нужны и стандартные зависимости Quarkus:

- `io.quarkus:quarkus-rest` и `io.quarkus:quarkus-rest-jackson` для webhook HTTP endpoint;
- `io.quarkus:quarkus-hibernate-validator` для валидации config properties;
- `io.quarkus:quarkus-redis-client` для `max.bot.storage.type=REDIS`.
