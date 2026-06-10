# Micronaut

`max-micronaut-starter` - Micronaut-обертка для MAX framework runtime. По возможностям она повторяет Spring Boot starter: properties, client/runtime wiring, polling, webhook, Redis storage, auto-registration для `@Route`, `@Screen`, `@ScreenController` и `@WidgetController`.

## Зависимости

Минимально:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-micronaut-starter:<version>")
}
```

Для webhook нужен HTTP server Micronaut, например:

```kotlin
dependencies {
    implementation("io.micronaut:micronaut-http-server-netty")
}
```

Для Redis storage:

```kotlin
dependencies {
    implementation("io.micronaut.redis:micronaut-redis-lettuce")
}
```

## Polling режим

`application.yml`:

```yaml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: POLLING
    polling:
      enabled: true
      limit: 100
      timeout: 30s
```

Минимальный `Router` bean:

```java
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;

@Factory
public final class BotConfig {
    @Singleton
    Router botRouter() {
        Router router = new Router("main");
        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text("Привет из Micronaut"));
            return CompletableFuture.completedFuture(null);
        });
        return router;
    }
}
```

Starter сам создает `MaxBotClient`, `Dispatcher` и lifecycle для polling. При `max.bot.mode=WEBHOOK` или `max.bot.polling.enabled=false` polling runtime pieces не создаются.

## Webhook режим

`application.yml`:

```yaml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: WEBHOOK
    webhook:
      path: /webhook/max
      secret: ${MAX_WEBHOOK_SECRET}
      max-in-flight: 64
```

В этом режиме starter поднимает Micronaut HTTP endpoint по пути `max.bot.webhook.path` и передает запросы в `WebhookReceiver`.

Проверенное поведение endpoint:

- корректный запрос возвращает `200 OK`
- неверный `X-Max-Bot-Api-Secret` возвращает `403 Forbidden`
- некорректный JSON возвращает `400 Bad Request`
- overload из `WebhookReceiver` возвращает `429 Too Many Requests`
- internal error из `WebhookReceiver` возвращает `500 Internal Server Error`

## Аннотационный роутинг

Классы с `@Route` можно регистрировать без отдельного bean declaration. Starter сканирует пакеты приложения и сам добавляет такие классы в runtime. Если у `@Route(autoRegister = true)`, route автоматически включается в `Dispatcher`.

```java
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Callback;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.message.Messages;

@Route(value = "menu", autoRegister = true)
public final class MenuRoute {

    @Command("start")
    public void start(RuntimeContext ctx) {
        ctx.reply(Messages.text("Команды: /menu"));
    }

    @Command("menu")
    public void menu(RuntimeContext ctx) {
        ctx.reply(Messages.text("Меню открыто"));
    }

    @Callback("menu:ok")
    public void ok(RuntimeContext ctx) {
        ctx.answerCallback("OK");
    }
}
```

Если route уже объявлен как явный Micronaut bean, starter тоже включает его в `Dispatcher` и сохраняет ordering semantics для `Router` beans.

## Storage

По умолчанию starter использует:

- `max.bot.storage.type=MEMORY`
- `max.bot.storage.state-scope=USER_IN_CHAT`

Переключение на Redis:

```yaml
max:
  bot:
    storage:
      type: REDIS
      redis:
        key-prefix: max:bot:fsm
        ttl: 30m
```

Для Redis path starter ожидает доступный `StatefulRedisConnection<String, String>` из Micronaut Redis Lettuce integration. Если `type=REDIS`, но нужной Redis integration нет, startup завершается с диагностикой. Memory path при этом не меняется.

## Screen API в Micronaut

Starter автоматически регистрирует классы с `@Screen(autoRegister = true)`.

```java
import java.util.Map;
import java.util.concurrent.CompletionStage;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.screen.annotation.OnAction;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

@Singleton
@Screen("home")
public final class HomeScreen {
    @Render
    public ScreenModel render(ScreenContext ctx) {
        return ScreenModel.builder()
                .title("Главная")
                .widget(Widgets.text("Экран зарегистрирован через Micronaut starter"))
                .showBackButton(false)
                .build();
    }

    @OnAction("open_profile")
    public CompletionStage<Void> openProfile(ScreenContext ctx) {
        return ctx.nav().push("profile", Map.of("name", "Гость"));
    }
}
```

Если у `@Screen(autoRegister = false)`, экран не попадает в `ScreenRegistry`. Невалидный `@Screen` приводит к startup failure.

## Screen controller facade

Для фасадного API используются аннотации:

- `ru.tardyon.botframework.micronaut.screen.annotation.ScreenController`
- `ru.tardyon.botframework.micronaut.screen.annotation.ScreenView`
- `ru.tardyon.botframework.micronaut.screen.annotation.OnScreenAction`
- `ru.tardyon.botframework.micronaut.screen.annotation.OnScreenText`

Пример:

```java
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.micronaut.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.micronaut.screen.annotation.OnScreenText;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenController;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenView;
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

- один контроллер может определять несколько screen views
- фасады автоматически регистрируются в `ScreenRegistry`
- autodetected top-level `@ScreenController` поддерживается без явного bean declaration
- невалидные сигнатуры вызывают startup failure

## Widget controller facade

Для widget facade используются:

- `ru.tardyon.botframework.micronaut.widget.annotation.WidgetController`
- `ru.tardyon.botframework.micronaut.widget.annotation.Widget`
- `ru.tardyon.botframework.micronaut.widget.annotation.OnWidgetAction`

Пример:

```java
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.micronaut.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.micronaut.widget.annotation.Widget;
import ru.tardyon.botframework.micronaut.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.WidgetContext;
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
    public CompletionStage<WidgetEffect> increment(WidgetContext context, String action, Map<String, String> args) {
        return CompletableFuture.completedFuture(WidgetEffect.RERENDER);
    }
}
```

## Demo project

Готовый пример лежит в [demo-micronaut-polling](../demo-micronaut-polling).

Запуск polling demo:

```bash
BOT_TOKEN=... ./gradlew :demo-micronaut-polling:run
```

Запуск с Redis storage:

```bash
BOT_TOKEN=... MICRONAUT_ENVIRONMENTS=redis REDIS_URI=redis://127.0.0.1:6379/0 ./gradlew :demo-micronaut-polling:run
```

Что показывает demo:

- manual `Router` bean со shared polling demo logic;
- auto-registered `@Route` classes, включая constructor-injected `ApiSmokeRoute`;
- auto-registered `@Screen`, `@ScreenController` и `@WidgetController`;
- polling runtime и Redis profile через `application-redis.yml`.

Проверенное поведение:

- widget controller автоматически регистрируется в runtime resolver
- callback buttons получают encoded action через registry
- `@WidgetController(autoRegister = false)` не регистрируется автоматически
- для неизвестного widget возвращается fallback view
- для неизвестного action dispatch возвращает `WidgetEffect.NONE`
- невалидные render/action сигнатуры и дубликаты приводят к startup/registration failure
