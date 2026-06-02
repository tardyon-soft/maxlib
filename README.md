# MAX Java Bot Framework

Java-библиотека для разработки ботов на платформе MAX.

Функциональность, которая есть в текущем коде библиотеки:

- типизированные модели и DTO для MAX API;
- HTTP-клиент `MaxBotClient`;
- runtime-слой `Dispatcher` и `Router`;
- фильтры, middleware и аннотационный роутинг;
- FSM, scene и wizard API;
- screen API, формы и widget-слой;
- Spring Boot starter для polling и webhook;
- testkit для проверки обработчиков и update-потока.

Библиотека собирается под Java 17.

## Модули

- `max-model` - модели MAX API.
- `max-client-core` - клиентский слой и HTTP transport.
- `max-fsm` - FSM storage, scene и wizard primitives.
- `max-dispatcher` - `Dispatcher`, `Router`, filters, middleware, screens, upload contracts.
- `max-spring-boot-starter` - Spring Boot auto-configuration.
- `max-testkit` - утилиты для тестирования.

## Быстрый выбор зависимости

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

Если нужен FSM, он уже приходит транзитивно через `max-dispatcher`.

## Документация

- [Установка и состав модулей](docs/installation.md)
- [Vanilla Java: клиент, dispatcher и long polling](docs/vanilla-java.md)
- [Spring Boot: polling, webhook и auto-registration](docs/spring-boot.md)
- [Dispatcher, Router, filters и middleware](docs/dispatcher-routing.md)
- [FSM, scenes, wizards, screens и forms](docs/fsm-screens.md)
- [Подробно: `@ScreenController`, `@WidgetController` и screen facade API](docs/screen-controllers.md)
- [Работа с `MaxBotClient` и upload API](docs/client-api.md)
- [Тестирование через `max-testkit`](docs/testing.md)

## Где смотреть живые примеры

- Spring Boot demo-приложение: `demo-spring-polling`
- Примеры аннотационного роутинга: `demo-spring-polling/src/main/java/ru/tardyon/botframework/demo/springpolling/AnnotatedMenuRoute.java`
- Примеры screen API: `demo-spring-polling/src/main/java/ru/tardyon/botframework/demo/springpolling/AnnotatedHomeScreen.java`
