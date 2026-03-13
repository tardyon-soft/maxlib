# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX с DX в стиле aiogram 3, адаптированный под Java ecosystem и реальные ограничения MAX API.

Важно: это не буквальная копия aiogram. Framework повторяет ergonomic-подходы там, где они совместимы с MAX.

Базовый namespace проекта: `ru.tardyon.botframework`.

## Quick Start

### 1) Требования

- JDK 21+
- Gradle wrapper (`./gradlew`)

### 2) Подключение

На текущем этапе проект используется как source-first framework (из checkout репозитория).

Для модульного использования внутри этого монорепо:

```kotlin
dependencies {
    implementation(project(":max-dispatcher"))
    implementation(project(":max-client-core"))
    implementation(project(":max-fsm"))
}
```

Для Spring Boot integration:

```kotlin
dependencies {
    implementation(project(":max-spring-boot-starter"))
}
```

### 3) Сборка

```bash
./gradlew clean test
```

### 3.1) Публикация в Maven Central

Для vanilla Java стека (без Spring starter):

```bash
./gradlew publishVanillaJavaToMavenCentral
```

Для starter-стека (включая Spring starter и его зависимости):

```bash
./gradlew publishStarterToMavenCentral
```

Требуемые переменные окружения для публикации:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_GPG_PRIVATE_KEY`
- `MAVEN_GPG_PASSPHRASE`

### 3.2) GitLab CI Pipeline

Добавлен `.gitlab-ci.yml` с поведением:

- при `push` в `master`:
  - `build` (`clean assemble`)
  - `unit_tests` (`test`)
- после успешных тестов доступна manual job `publish_maven_central`:
  - запускает `publishVanillaJavaToMavenCentral` и `publishStarterToMavenCentral`.

Для manual publish в GitLab CI/CD Variables должны быть заданы:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_GPG_PRIVATE_KEY`
- `MAVEN_GPG_PASSPHRASE`

### 4) Минимальный runtime bot (без Spring)

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

### 5) Минимальный Spring Boot bot (polling)

```yaml
# application.yml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: POLLING
    polling:
      enabled: true
      limit: 100
      timeout: 30s
      types: [message_created, message_callback]
```

```java
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;

@SpringBootApplication
class BotApp {
    @Bean
    Router botRouter() {
        Router router = new Router("main");
        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text("Привет из polling mode"));
            return CompletableFuture.completedFuture(null);
        });
        return router;
    }
}
```

### 6) Минимальный Spring Boot bot (webhook)

```yaml
# application.yml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: WEBHOOK
    webhook:
      enabled: true
      path: /webhook/max
      secret: ${MAX_BOT_WEBHOOK_SECRET}
      max-in-flight: 64
```

Starter поднимет webhook endpoint и передаст update в тот же dispatcher pipeline.

## Architecture Overview

Framework состоит из трёх основных уровней:

1. Client SDK (`max-client-core`, `max-model`)
- typed requests/responses
- HTTP transport, auth, serialization, error model

2. Runtime (`max-dispatcher`, `max-fsm`)
- ingestion (polling/webhook)
- dispatcher/router/handlers
- filters + middleware + runtime context enrichment
- DI parameter resolution
- high-level messaging/media API
- FSM/scenes/wizard

3. Integration & Testing (`max-spring-boot-starter`, `max-testkit`)
- Spring Boot autoconfiguration + polling/webhook bootstrap
- test harness + fixtures + side-effect capture

## Module Overview

- `max-client-core`: typed MAX API client, transport/auth/serialization/errors.
- `max-model`: DTO, enums, value objects.
- `max-dispatcher`: ingestion + dispatcher/router + filters/middleware + DI + messaging/media runtime.
- `max-fsm`: FSM storage abstractions, state scopes, scenes, wizard.
- `max-spring-boot-starter`: Spring Boot properties/autoconfiguration, polling/webhook adapter wiring.
- `max-testkit`: runtime testing helpers (`DispatcherTestKit`, `UpdateFixtures`, `RecordingMaxBotClient`).
- `demo-spring-polling`: отдельное Spring Boot demo-приложение для ручной проверки framework в polling mode.

## Project Status

Sprint 9 завершён: framework включает core runtime, Spring Boot starter, testkit и рабочие examples.

Что стабилизировано после Sprint 9:

- public starter properties/autoconfiguration surface;
- polling/webhook Spring integration over framework-agnostic ingestion;
- testkit harness + fixtures для handler/runtime testing;
- docs и contracts синхронизированы с фактическим API.

## Polling vs Webhook

- Polling: `LongPollingRunner` + `PollingUpdateSource`, lifecycle-managed (`start/stop/shutdown`).
- Webhook: framework-agnostic `WebhookReceiver` + secret validation (`X-Max-Bot-Api-Secret`).
- Оба источника сходятся в единый internal update pipeline и одинаковый dispatcher flow.

## Filters and Middleware

Pipeline order:

1. outer middleware
2. filter evaluation
3. inner middleware
4. handler invocation

Базовые built-in filters:

- `BuiltInFilters.command("start")`
- `BuiltInFilters.textEquals(...)`
- `BuiltInFilters.textStartsWith(...)`
- `BuiltInFilters.chatType(...)`
- `BuiltInFilters.fromUser(...)`
- `BuiltInFilters.hasAttachment()`
- `BuiltInFilters.state(...)`

## DI and Handler Invocation

Framework вызывает handler-ы через invocation engine (`HandlerInvoker` + `HandlerParameterResolver`).

Поддерживаются параметры из:

- runtime objects (`RuntimeContext`, `Update`, `Message`, `Callback`, `User`, `Chat`)
- filter/middleware enrichment data
- shared services (`Dispatcher.registerService(...)`)
- FSM/scenes (`FSMContext`, `SceneManager`, `WizardManager`)

## Messaging, Keyboards, Callbacks

High-level API:

- `MessagingFacade` (`send/edit/delete/reply`)
- `Messages` / `MessageBuilder`
- `Keyboards` / `KeyboardBuilder` / `Buttons`
- `CallbackFacade` / `CallbackContext`
- `ChatActionsFacade`

Runtime shortcuts через `RuntimeContext`:

- `ctx.reply(...)`
- `ctx.answerCallback(...)`
- `ctx.chatAction(...)`

## Upload / Media

Upload/media слой:

- `InputFile.fromPath / fromBytes / fromStream`
- `UploadService` (`prepare -> transfer -> finalize`)
- multipart + resumable upload modes
- normalized `UploadResult`
- `MediaMessagingFacade` (`sendImage/sendFile/sendVideo/sendAudio`, reply variants)

## FSM / Scenes

Состояние и диалоги:

- `FSMStorage` + `MemoryStorage`
- `FSMContext`
- state scopes: `USER`, `CHAT`, `USER_IN_CHAT`
- `StateFilter`
- `SceneRegistry`, `SceneManager`
- `WizardManager` (`enter/next/back/exit`)

## Testing Story

`max-testkit` даёт быстрый runtime testing без ручного boilerplate:

- `DispatcherTestKit` для setup и dispatch
- `UpdateFixtures` DSL (`message`, `callback`, `statefulMessages`)
- `RecordingMaxBotClient` + `CapturedApiCall` для assertions side effects

Пример:

```java
Router router = new Router("test");
router.message((message, ctx) -> {
    ctx.reply(Messages.text("pong"));
    return java.util.concurrent.CompletableFuture.completedFuture(null);
});

DispatcherTestKit kit = DispatcherTestKit.withRouter(router);
DispatcherTestKit.DispatchProbe probe = kit.feedAndCapture(UpdateFixtures.message().text("ping").build());
```

## Examples

- `examples/sprint-2-low-level`: polling/webhook ingestion low-level.
- `examples/sprint-3-runtime`: dispatcher/router basics.
- `examples/sprint-4-filters-middleware`: filters + middleware.
- `examples/sprint-5-di-invocation`: DI/invocation signatures.
- `examples/sprint-6-messaging`: messages/keyboards/callbacks/actions.
- `examples/sprint-7-upload-media`: upload/media runtime usage.
- `examples/sprint-9-e2e`: polished end-to-end examples (Spring polling/webhook + runtime scenarios).
- `demo-spring-polling`: runnable Spring Boot app (`./gradlew :demo-spring-polling:run`) для ручного smoke/manual QA.

## Current Limitations

- MAX API surface покрыт частично; расширение endpoints продолжается.
- Rich magic DSL в стиле aiogram не реализуется как цель сама по себе; приоритет — явный typed API.
- Upload resumable state пока runtime-local (без distributed persistent resume manager).
- `GET /videos/{videoToken}` helper layer пока не вынесен в отдельный read-model abstraction.
- Spring starter не включает отдельный enterprise observability/deployment toolkit.

## Future Work (V2+)

- расширение SDK surface по дополнительным MAX endpoints;
- дополнительные persistent storages для FSM/scenes;
- расширение testkit assertions/helpers без дублирования runtime;
- optional observability integrations (logging/metrics/tracing adapters).

## Documentation

- Product vision: [docs/product-spec.md](docs/product-spec.md)
- API contract: [docs/api-contract.md](docs/api-contract.md)
- Runtime contract: [docs/runtime-contract.md](docs/runtime-contract.md)
- Filters/middleware: [docs/filters-and-middleware.md](docs/filters-and-middleware.md)
- DI/invocation: [docs/di-and-invocation.md](docs/di-and-invocation.md)
- Messaging API: [docs/messaging-api.md](docs/messaging-api.md)
- Upload/media: [docs/upload-and-media.md](docs/upload-and-media.md)
- FSM/scenes: [docs/fsm-and-scenes.md](docs/fsm-and-scenes.md)
- Spring starter: [docs/spring-starter.md](docs/spring-starter.md)
- Testkit: [docs/testkit.md](docs/testkit.md)
- Roadmap: [docs/roadmap.md](docs/roadmap.md)
- Contributing: [docs/contributing.md](docs/contributing.md)
- ADR index: [docs/adr/README.md](docs/adr/README.md)

## Source of Truth

- MAX API docs:
  - https://dev.max.ru/docs-api
  - https://dev.max.ru/docs-api/methods
  - https://dev.max.ru/docs-api/objects
- aiogram reference (DX patterns):
  - https://docs.aiogram.dev/
