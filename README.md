# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX.

## Maven namespace

Используйте артефакты с `groupId`:

- `ru.tardyon.maven`

Пакеты в Java-коде остаются `ru.tardyon.botframework.*`.

## Модули

- `max-model` — типизированные модели MAX API.
- `max-client-core` — HTTP/serialization client.
- `max-dispatcher` — `Dispatcher`, `Router`, filters/middleware, annotation routes, screen API.
- `max-fsm` — FSM storage/scope/state.
- `max-spring-boot-starter` — Spring Boot auto-configuration (polling/webhook/dispatcher/storage).
- `max-testkit` — тестовые утилиты.
- `demo-spring-polling` — живое demo-приложение.

## Установка

Gradle:

```kotlin
dependencies {
    implementation("ru.tardyon.maven:max-spring-boot-starter:<version>")
}
```

Maven:

```xml
<dependency>
  <groupId>ru.tardyon.maven</groupId>
  <artifactId>max-spring-boot-starter</artifactId>
  <version>${maxlib.version}</version>
</dependency>
```

Без Spring:

```kotlin
dependencies {
    implementation("ru.tardyon.maven:max-dispatcher:<version>")
    implementation("ru.tardyon.maven:max-client-core:<version>")
    implementation("ru.tardyon.maven:max-fsm:<version>")
}
```

## Spring Boot quick start (Polling)

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
      types:
        - message_created
        - message_callback
```

Базовый router:

```java
@Bean
Router botRouter() {
    Router router = new Router("main");
    router.message(BuiltInFilters.command("start"), (message, ctx) -> {
        ctx.reply(Messages.text("Привет из MAX bot"));
        return CompletableFuture.completedFuture(null);
    });
    return router;
}
```

`LongPollingRunnerConfig` теперь создается starter-ом автоматически, отдельный `@Bean` в приложении не нужен.

## Аннотационный API (синтаксический sugar)

```java
@Route(value = "menu", autoRegister = true)
public class MenuRoute {
    @Command("start")
    public void start(RuntimeContext ctx) {
        ctx.reply(Messages.text("Откройте /menu"));
    }

    @Command("menu")
    public CompletionStage<Void> menu(RuntimeContext ctx) {
        ctx.reply(Messages.text("Меню готово"));
        return CompletableFuture.completedFuture(null);
    }

    @Callback("menu:pay")
    public void pay(Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Оплата подтверждена");
    }

    @CallbackPrefix("menu:")
    public void fallback(Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Неизвестный пункт");
    }
}
```

Поддерживаются:

- `@Route(value, autoRegister)`
- `@Message(text, startsWith)`
- `@Text(...)`
- `@Command(...)`
- `@Callback(...)`
- `@CallbackPrefix(...)`
- `@State(...)`
- `@UseFilters(...)`
- `@UseMiddleware(...)`

Важно:

- Это слой поверх существующего `Router` API.
- Старый способ (`Router` + `BuiltInFilters`) полностью сохранен.
- В Spring starter классы с `@Route` автоматически регистрируются как beans (без `@Component`).

## Handler сигнатуры

Поддерживаемые параметры: `RuntimeContext`, `Update`, `Message`, `Callback`, `User`, `Chat`, enrichment-значения, сервисы из `Dispatcher.registerService(...)`, FSM/scene/wizard зависимости.

Возвращаемый тип:

- `void`
- `CompletionStage<?>`

## Команды и приоритет

`BuiltInFilters.command(...)` выполняется с повышенным приоритетом фильтра, чтобы команды обрабатывались раньше generic text/state handlers.

## Screens API

Есть два способа:

- ручная регистрация `ScreenDefinition` в `ScreenRegistry`;
- аннотации `@Screen`, `@Render`, `@OnAction`, `@OnText`.

Для screen-flow используется отдельный FSM namespace `screen` (`context.fsm("screen")`), чтобы стейты экранов не конфликтовали с пользовательскими FSM стейтами.

Виджеты поддерживают текст/кнопки и media attachments (`Widgets.media(...)`, `Widgets.attachment(...)`, `Widgets.attachments(...)`), но для MAX нужно передавать валидные upload/file reference.

Пример запуска screen-flow из команды:

```java
router.message(BuiltInFilters.command("screen"), (message, ctx) ->
        Screens.navigator(ctx, screenRegistry).start("home", Map.of())
);
```

Пример ручного экрана:

```java
screenRegistry.register(new ScreenDefinition() {
    @Override
    public String id() {
        return "home";
    }

    @Override
    public CompletionStage<ScreenModel> render(ScreenContext context) {
        return CompletableFuture.completedFuture(
                ScreenModel.builder()
                        .title("Главная")
                        .widget(Widgets.text("Добро пожаловать"))
                        .widget(Widgets.buttonRow(ScreenButton.of("Профиль", "open_profile")))
                        .showBackButton(false)
                        .build()
        );
    }

    @Override
    public CompletionStage<Void> onAction(ScreenContext context, String action, Map<String, String> args) {
        if ("open_profile".equals(action)) {
            return context.nav().push("profile", Map.of("name", "Гость"));
        }
        return CompletableFuture.completedFuture(null);
    }
});
```

Пример аннотационного экрана:

```java
@Screen("profile")
public final class ProfileScreen {
    @Render
    public ScreenModel render(ScreenContext ctx) {
        String name = String.valueOf(ctx.params().getOrDefault("name", "Гость"));
        return ScreenModel.builder()
                .title("Профиль")
                .widget(Widgets.text("Имя: " + name))
                .showBackButton(true)
                .build();
    }

    @OnText
    public CompletionStage<Void> onText(ScreenContext ctx, String text) {
        String next = text == null || text.isBlank() ? "Гость" : text.trim();
        return ctx.nav().replace("profile", Map.of("name", next));
    }
}
```

## Storage

Поддержка FSM storage:

- `MEMORY` (по умолчанию)
- `REDIS` (через `spring-boot-starter-data-redis` и `max.bot.storage.type=REDIS`)

## MAX API docs compatibility

- Матрица покрытия endpoint-ов: [docs/max-api-coverage-matrix.md](docs/max-api-coverage-matrix.md)
- Новые `*Api` методы клиентского слоя повторяют transport shape docs-api.
- Legacy-методы сохранены как совместимый sugar слой.

## Документация и демо

- Demo приложение: [demo-spring-polling/README.md](demo-spring-polling/README.md)
- ADR: [docs/adr/README.md](docs/adr/README.md)

## Тестирование (max-testkit)

Полезные утилиты:

- `DispatcherTestKit`
- `UpdateFixtures`
- `RecordingMaxBotClient`

Пример:

```java
Router router = new Router("test");
router.message((message, ctx) -> {
    ctx.reply(ru.tardyon.botframework.message.Messages.text("pong"));
    return java.util.concurrent.CompletableFuture.completedFuture(null);
});

ru.tardyon.botframework.testkit.DispatcherTestKit kit =
        ru.tardyon.botframework.testkit.DispatcherTestKit.withRouter(router);

var probe = kit.feedAndCapture(ru.tardyon.botframework.testkit.UpdateFixtures.message().text("ping").build());
```

## Локальная сборка репозитория

```bash
./gradlew clean test
```

## Публикация с вашим namespace

Если вы публикуете как `ru.tardyon.maven`, проверьте перед release:

1. `group`/`artifactId` в publish-конфигурации соответствуют вашему namespace.
2. Версии модулей выровнены.
3. Все модули (`max-model`, `max-client-core`, `max-fsm`, `max-dispatcher`, `max-spring-boot-starter`, `max-testkit`) опубликованы в один репозиторий.

## Примеры в репозитории

- `examples/sprint-2-low-level`
- `examples/sprint-3-runtime`
- `examples/sprint-4-filters-middleware`
- `examples/sprint-5-di-invocation`
- `examples/sprint-6-messaging`
- `examples/sprint-7-upload-media`
- `examples/sprint-9-e2e`
- `demo-spring-polling` (включая demo аннотационного API)

## Документация

- [docs/product-spec.md](docs/product-spec.md)
- [docs/api-contract.md](docs/api-contract.md)
- [docs/runtime-contract.md](docs/runtime-contract.md)
- [docs/spring-starter.md](docs/spring-starter.md)
- [docs/testkit.md](docs/testkit.md)
