# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX.

README ниже ориентирован на использование библиотеки как опубликованных Maven-артефактов с вашим namespace:

- `groupId`: `ru.tardyon.maven`

Если у вас в репозитории артефакты публикуются под этим namespace, используйте координаты именно в таком виде.

## Что дает framework

- typed MAX client (`max-client-core`, `max-model`);
- runtime dispatcher/router (`max-dispatcher`);
- FSM/scenes/wizard (`max-fsm`);
- Spring Boot starter (`max-spring-boot-starter`);
- testkit для unit/integration тестов (`max-testkit`).

## Совместимость с docs-api MAX

- Сводная матрица endpoint coverage: [docs/max-api-coverage-matrix.md](docs/max-api-coverage-matrix.md)
- Принцип совместимости:
  - старые методы (`LEGACY`) сохраняются как синтаксический сахар/нормализованный API;
  - новые методы `*Api` дают docs-совместимый transport-контракт (path/query/body/shape).

## Требования

- JDK 21+
- Gradle 8+/9+ или Maven 3.9+

## Установка зависимостей (namespace `ru.tardyon.maven`)

### 1) Spring Boot проект (рекомендуется)

Gradle:

```kotlin
dependencies {
    implementation("ru.tardyon.maven:max-spring-boot-starter:0.1.0")
}
```

Maven:

```xml
<dependency>
  <groupId>ru.tardyon.maven</groupId>
  <artifactId>max-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2) Vanilla Java runtime (без Spring)

Gradle:

```kotlin
dependencies {
    implementation("ru.tardyon.maven:max-dispatcher:0.1.0")
    implementation("ru.tardyon.maven:max-client-core:0.1.0")
    implementation("ru.tardyon.maven:max-fsm:0.1.0")
}
```

Maven:

```xml
<dependency>
  <groupId>ru.tardyon.maven</groupId>
  <artifactId>max-dispatcher</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>ru.tardyon.maven</groupId>
  <artifactId>max-client-core</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>ru.tardyon.maven</groupId>
  <artifactId>max-fsm</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Быстрый старт: Spring Boot (polling)

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

Простой router-bean:

```java
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;

@SpringBootApplication
public class BotApplication {
    @Bean
    Router botRouter() {
        Router router = new Router("main");
        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text("Привет из MAX bot"));
            return CompletableFuture.completedFuture(null);
        });
        return router;
    }
}
```

## Быстрый старт: аннотационный API (sugar поверх Router)

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Callback;
import ru.tardyon.botframework.dispatcher.annotation.CallbackPrefix;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.message.Messages;

@Route(value = "menu", autoRegister = true)
public class MenuRoute {

    @Command("start")
    public CompletionStage<Void> start(RuntimeContext ctx) {
        ctx.reply(Messages.text("Откройте /menu"));
        return CompletableFuture.completedFuture(null);
    }

    @Command("menu")
    public void menu(RuntimeContext ctx) {
        ctx.reply(Messages.text("Меню готово"));
    }

    @Callback("menu:pay")
    public void pay(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Оплата подтверждена");
    }

    @CallbackPrefix("menu:")
    public void fallback(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Неизвестный пункт меню");
    }
}
```

Поддерживаемые аннотации:

- `@Route(value, autoRegister=true|false)`
- `@Message(text, startsWith)`
- `@Text(...)`
- `@Command(...)`
- `@Callback(...)`
- `@CallbackPrefix(...)`
- `@State(...)`
- `@UseFilters(...)`
- `@UseMiddleware(...)`

Важно:

- Это дополнительный синтаксический слой.
- Старый API через `Router`/`BuiltInFilters` работает без изменений.
- В Spring starter `@Route` автоматически поднимается как bean (без обязательного `@Component`).

## Быстрый старт: vanilla Java (без Spring)

```java
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;

Dispatcher dispatcher = new Dispatcher()
        .withBotClient(configuredMaxBotClient());

Router router = new Router("main");
router.message((message, ctx) -> {
    ctx.reply(Messages.text("Привет, MAX!"));
    return CompletableFuture.completedFuture(null);
});

dispatcher.includeRouter(router);
```

## Основные runtime-концепции

### Dispatcher / Router

- `Dispatcher` — корневой orchestration слой;
- `Router` — модуль регистрации handlers;
- поддерживается router tree через `includeRouter(...)`;
- first-match routing + deterministic traversal.

### Filters / Middleware

Порядок pipeline:

1. outer middleware
2. filter evaluation
3. inner middleware
4. handler invocation

Базовые фильтры:

- `BuiltInFilters.command("start")`
- `BuiltInFilters.textEquals(...)`
- `BuiltInFilters.textStartsWith(...)`
- `BuiltInFilters.callbackDataEquals(...)`
- `BuiltInFilters.callbackDataStartsWith(...)`
- `BuiltInFilters.state(...)`

### DI и сигнатуры handler-ов

Handler может получать:

- `RuntimeContext`, `Update`, `Message`, `Callback`, `User`, `Chat`
- filter/middleware enrichment данные
- сервисы, зарегистрированные через `Dispatcher.registerService(...)`
- FSM объекты (`FSMContext`, `SceneManager`, `WizardManager`)

Допустимые возвращаемые типы handler-методов:

- `void`
- `CompletionStage<?>`

## Spring режимы

### Polling

- `max.bot.mode=POLLING`
- starter поднимет `LongPollingRunner` и будет доставлять updates в `Dispatcher`

### Webhook

- `max.bot.mode=WEBHOOK`
- starter поднимет webhook endpoint и тот же dispatcher pipeline

## Тестирование

Для тестов используйте `max-testkit`:

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
