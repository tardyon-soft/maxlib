# Quarkus

`max-quarkus-starter` - Quarkus-обертка для MAX framework runtime. По поведению он повторяет Spring Boot и Micronaut starters в пределах текущей Quarkus CDI/REST реализации: properties, core wiring, polling, webhook, Redis storage, auto-registration для `@Route`, `@Screen`, `@ScreenController` и `@WidgetController`.

## Dependencies

Минимально:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-quarkus-starter:<version>")
}
```

Для webhook нужен Quarkus REST слой:

```kotlin
dependencies {
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
}
```

Для Redis storage:

```kotlin
dependencies {
    implementation("io.quarkus:quarkus-redis-client")
}
```

Если `max.bot.storage.type=REDIS`, Quarkus starter ищет `RedisDataSource`. Без него startup завершается с диагностикой.

## Polling

Polling runtime активен по умолчанию. Фактическое условие в текущей реализации такое:

- `max.bot.mode` отсутствует или равен `POLLING`
- `max.bot.polling.enabled` отсутствует или равен `true`

Пример:

```yaml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: POLLING
    polling:
      enabled: true
      timeout: 30s
      limit: 100
```

Quarkus starter создает:

- `PollingUpdateSource`
- `LongPollingRunnerConfig`
- `LongPollingRunner`
- lifecycle bridge для `start/stop/shutdown`

Если задать `max.bot.mode=WEBHOOK` или `max.bot.polling.enabled=false`, эти runtime pieces не публикуются.

## Webhook

Webhook runtime включается только когда:

- `max.bot.mode=WEBHOOK`, или
- `max.bot.webhook.enabled=true`

Пример:

```yaml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: WEBHOOK
    webhook:
      enabled: true
      path: /hooks/max
      secret: ${MAX_WEBHOOK_SECRET}
      max-in-flight: 64
```

Поддерживаемое поведение endpoint:

- `ACCEPTED` -> `200 OK`
- `INVALID_SECRET` -> `403 Forbidden`
- `BAD_PAYLOAD` -> `400 Bad Request`
- `OVERLOADED` -> `429 Too Many Requests`
- `INTERNAL_ERROR` -> `500 Internal Server Error`

Если webhook выключен, endpoint возвращает `404 Not Found`.

## Redis storage

FSM storage по умолчанию работает в memory mode:

```yaml
max:
  bot:
    storage:
      type: MEMORY
```

Redis mode:

```yaml
max:
  bot:
    storage:
      type: REDIS
      redis:
        key-prefix: max:bot:fsm
        ttl: 30m
```

Поведение:

- `type=MEMORY` -> `MemoryStorage`
- `type=REDIS` -> `RedisFSMStorage`
- `key-prefix` и `ttl` прокидываются в Redis-backed storage
- при отсутствии `RedisDataSource` startup fails with clear error

## Route auto-registration

`@Route(autoRegister = true)` поддерживается через Quarkus-friendly discovery:

- сначала Jandex/indexed metadata
- затем fallback scan по classpath для `.class` под package prefix `ru.tardyon.botframework.*`

Пример:

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;

@Route(value = "menu", autoRegister = true)
public final class MenuRoute {
    @Command("start")
    public CompletionStage<Void> start() {
        return CompletableFuture.completedFuture(null);
    }
}
```

Если `autoRegister=false`, discovery находит класс, но не регистрирует его в `Dispatcher`.

## Screen API

`@Screen(autoRegister = true)` регистрируется в `ScreenRegistry` автоматически.

Пример:

```java
import java.util.Map;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.screen.annotation.OnAction;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

@Screen("home")
public final class HomeScreen {
    @Render
    public ScreenModel render(ScreenContext context) {
        return ScreenModel.builder()
                .title("Главная")
                .widget(Widgets.text("Экран зарегистрирован через Quarkus starter"))
                .build();
    }

    @OnAction("open_profile")
    public void openProfile(ScreenContext context) {
        context.nav().push("profile", Map.of("name", "Guest"));
    }
}
```

Если `autoRegister=false`, экран не попадает в `ScreenRegistry`.

## Screen controllers

Фасадный API для screen controller реализован аннотациями:

- `@ScreenController`
- `@ScreenView`
- `@OnScreenAction`
- `@OnScreenText`

Пример:

```java
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.quarkus.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.quarkus.screen.annotation.OnScreenText;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenController;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenView;
import ru.tardyon.botframework.screen.ScreenModel;

@ScreenController
public final class ProfileScreenController {
    @ScreenView(screen = "profile.home")
    public ScreenModel home() {
        return ScreenModel.builder().title("Профиль").build();
    }

    @OnScreenText(screen = "profile.home")
    public void onText(String text) {
    }

    @OnScreenAction(screen = "profile.home", action = "refresh")
    public CompletionStage<Void> onRefresh(Map<String, String> args) {
        return CompletableFuture.completedFuture(null);
    }
}
```

Проверенное поведение:

- один controller может определять несколько screens
- `@ScreenView` обязателен для каждого registered screen
- неизвестный action no-op и возвращает completed `CompletionStage<Void>`
- отсутствие `@OnScreenText` handler тоже no-op
- invalid signatures fail at startup
- autodetected top-level `@ScreenController` beans поддерживаются

## Widget controllers

Фасадный API для widget controller реализован аннотациями:

- `@WidgetController`
- `@Widget`
- `@OnWidgetAction`

Пример:

```java
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.quarkus.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.quarkus.widget.annotation.Widget;
import ru.tardyon.botframework.quarkus.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;

@WidgetController
public final class CounterWidgetController {
    @Widget(id = "counter.widget")
    public WidgetView render() {
        return WidgetView.of(
                List.of("Counter"),
                List.of(List.of(ScreenButton.of("Increment", "increment")))
        );
    }

    @OnWidgetAction(widget = "counter.widget", action = "increment")
    public CompletionStage<WidgetEffect> increment(Map<String, String> args) {
        return CompletableFuture.completedFuture(WidgetEffect.RERENDER);
    }
}
```

## Demo project

Готовый пример лежит в [demo-quarkus-polling](../demo-quarkus-polling).

Запуск polling demo:

```bash
BOT_TOKEN=... ./gradlew :demo-quarkus-polling:run
```

Запуск с Redis profile:

```bash
BOT_TOKEN=... QUARKUS_PROFILE=redis QUARKUS_REDIS_HOSTS=redis://127.0.0.1:6379/0 ./gradlew :demo-quarkus-polling:run
```

Что показывает demo:

- manual `Router` bean со shared polling demo logic;
- auto-registered `@Route` classes, включая bean-backed `ApiSmokeRoute`;
- auto-registered `@ScreenController` и `@WidgetController`;
- polling runtime и Redis profile через `application-redis.yml`.

Проверенное поведение:

- `@Widget` methods return `WidgetView` or `CompletionStage<WidgetView>`
- action handlers return `void`, `Void`, `WidgetEffect` or `CompletionStage` of those
- missing widget -> fallback view with text `Widget not found: <id>`
- unknown action -> `WidgetEffect.NONE`
- callback buttons are re-encoded through `WidgetActions.callbackAction(widgetId, action)`
- non-callback buttons remain unchanged
