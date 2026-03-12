# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX с DX в стиле aiogram 3.

Важно: это не буквальная копия aiogram 3. Проект адаптирует лучшие архитектурные идеи под реальные возможности MAX API и Java ecosystem.

## Sprint status

Текущий этап: `Sprint 2 — polling + webhook ingestion layer`.

Sprint 1 (`client/DTO/errors`) завершён.

Что уже реализовано:
- multi-module Gradle проект (Kotlin DSL) на Java 21;
- `max-client-core` foundation слой (transport, auth, serialization, errors, retry/rate-limit hooks, pagination);
- `max-model` с базовыми DTO, typed value objects и enum-контрактами;
- ingestion target contract в `max-dispatcher`: `UpdateSink` (async) + `UpdateHandlingResult` для unified polling/webhook flow;
- polling source abstraction в `max-dispatcher`: `PollingUpdateSource` + `SdkPollingUpdateSource` (SDK-backed `getUpdates` pull);
- long polling runtime foundation: `DefaultLongPollingRunner` с lifecycle API (`start/stop/isRunning`);
- marker progression contract: monotonic marker state с продвижением только после успешного batch handling;
- webhook secret validation foundation: `WebhookSecretValidator` + typed validation result/error contracts;
- domain-level операции в client SDK: `getMe`, message operations, callback answer, `getUpdates`, webhook subscriptions;
- тестовая инфраструктура client SDK: JSON fixtures + reusable mocked HTTP context.

## Modules

- `max-client-core` — Java SDK поверх MAX API.
- `max-model` — DTO, enum и value objects для MAX domain.
- `max-dispatcher` — заготовка runtime-слоя dispatcher/router.
- `max-fsm` — заготовка FSM abstractions.
- `max-spring-boot-starter` — заготовка Spring Boot integration.
- `max-testkit` — заготовка framework test utilities.

## Quick start (client SDK foundation)

```java
import java.time.Duration;
import okhttp3.OkHttpClient;
import ru.max.botframework.client.DefaultMaxBotClient;
import ru.max.botframework.client.MaxApiClientConfig;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.client.http.MaxHttpClient;
import ru.max.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.model.BotInfo;

MaxApiClientConfig config = MaxApiClientConfig.builder()
    .token("YOUR_BOT_TOKEN")
    .connectTimeout(Duration.ofSeconds(5))
    .readTimeout(Duration.ofSeconds(30))
    .build();

OkHttpClient okHttp = new OkHttpClient.Builder()
    .connectTimeout(config.connectTimeout())
    .readTimeout(config.readTimeout())
    .build();

MaxHttpClient httpClient = new OkHttpMaxHttpClient(config.baseUri(), okHttp);
MaxBotClient botClient = new DefaultMaxBotClient(config, httpClient, new JacksonJsonCodec());

BotInfo me = botClient.getMe();
```

## Client configuration

`MaxApiClientConfig` поддерживает builder-style конфигурацию:
- `baseUrl` / `baseUri` (по умолчанию `https://api.max.ru`);
- `token` (обязательный);
- `connectTimeout`, `readTimeout`;
- `userAgent`;
- `retryPolicy` (консервативный retry hook);
- `rateLimiter` (легковесный hook для client-side pacing).

Пример:

```java
import java.time.Duration;
import ru.max.botframework.client.MaxApiClientConfig;
import ru.max.botframework.client.RequestRateLimiter;
import ru.max.botframework.client.RetryPolicy;

MaxApiClientConfig config = MaxApiClientConfig.builder()
    .baseUrl("https://api.max.ru")
    .token("YOUR_BOT_TOKEN")
    .connectTimeout(Duration.ofSeconds(5))
    .readTimeout(Duration.ofSeconds(30))
    .userAgent("my-max-bot/1.0")
    .retryPolicy(RetryPolicy.fixed(2, Duration.ofMillis(200)))
    .rateLimiter(RequestRateLimiter.cooldown(Duration.ofMillis(300)))
    .build();
```

## Supported operations (Sprint 1)

`MaxBotClient` сейчас предоставляет:
- generic execution: `execute(MaxRequest<T>)` + `executeAsync(...)`;
- bot info: `getMe`, `getMeAsync`;
- messages:
  - `sendMessage`, `sendMessageAsync`;
  - `editMessage`, `editMessageAsync`;
  - `deleteMessage`, `deleteMessageAsync`;
  - `getMessage`, `getMessageAsync`;
  - `getMessages(ChatId)` и `getMessages(List<MessageId>)`;
- callbacks:
  - `answerCallback`, `answerCallbackAsync`;
- updates/polling foundation:
  - `getUpdates`, `getUpdatesAsync` (`marker`, `timeout`, `limit`, `types`);
- webhook subscriptions foundation:
  - `getSubscriptions`;
  - `createSubscription`, `createSubscriptionAsync`;
  - `deleteSubscription`, `deleteSubscriptionAsync`.

Сопутствующие foundation-возможности:
- HTTP transport: GET/POST/PUT/PATCH/DELETE;
- centralized JSON serialization (`JacksonJsonCodec` + shared mapper);
- auth interceptor (`Authorization` header);
- typed error hierarchy + structured MAX error payload;
- marker-based pagination abstractions;
- retry policy hook + rate-limit awareness (`429`, `Retry-After`).

## Current limitations

Ограничения текущего этапа (Sprint 2):
- сейчас фиксируется только ingestion transport contract (polling/webhook -> unified sink);
- framework runtime слой ещё не реализован: нет production-готовых `Dispatcher/Router`, filters DSL, middleware chain, DI runtime, FSM/scenes runtime;
- Spring Boot starter и testkit пока на уровне скелетов модулей;
- upload/media pipeline ещё не реализован;
- long-polling/webhook transport реализации как runtime-компоненты пока не завершены (зафиксирован контракт слоя ingestion);
- webhook runtime source/receiver пока не реализованы (доступен только framework-agnostic secret validator contract);
- surface MAX API покрыт частично и будет расширяться в следующих спринтах.

## Low-level Long Polling Example

```java
import java.time.Duration;
import ru.max.botframework.ingestion.DefaultLongPollingRunner;
import ru.max.botframework.ingestion.LongPollingRunner;
import ru.max.botframework.ingestion.LongPollingRunnerConfig;
import ru.max.botframework.ingestion.PollingFetchRequest;
import ru.max.botframework.ingestion.SdkPollingUpdateSource;
import ru.max.botframework.ingestion.UpdateSink;
import ru.max.botframework.model.UpdateEventType;

SdkPollingUpdateSource source = new SdkPollingUpdateSource(botClient);
UpdateSink sink = update -> {
    System.out.println("Update: " + update.updateId().value());
    return java.util.concurrent.CompletableFuture.completedFuture(
        ru.max.botframework.ingestion.UpdateHandlingResult.success()
    );
};

LongPollingRunner runner = new DefaultLongPollingRunner(
    source,
    sink,
    LongPollingRunnerConfig.builder()
        .request(new PollingFetchRequest(
            null,
            30,
            100,
            java.util.List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK)
        ))
        .idleDelay(Duration.ofMillis(100))
        .sourceErrorDelay(Duration.ofSeconds(1))
        .sinkErrorDelay(Duration.ofMillis(200))
        .build()
);

runner.start();
// ...
runner.stop();
```

## Marker Strategy (Long Polling)

- marker хранится внутри runner через `PollingMarkerState` (по умолчанию in-memory);
- marker продвигается только после успешной обработки всего полученного batch;
- при sink/source ошибках marker не двигается, чтобы сохранить at-least-once доставку;
- marker не регрессирует даже если source вернул более старое значение.

## Webhook Secret Validation

- заголовок секрета: `X-Max-Bot-Api-Secret`;
- контракт: `WebhookSecretValidator.validate(WebhookUpdatePayload)`;
- outcomes:
- `ACCEPTED`
- `SKIPPED_NO_SECRET_CONFIGURED`
- `REJECTED` (`SECRET_HEADER_MISSING` или `SECRET_MISMATCH`).

## Build and test

Требование: JDK 21+

```bash
./gradlew clean test
```

## Documentation

- Product vision and target DX: [docs/product-spec.md](docs/product-spec.md)
- Core API contract: [docs/api-contract.md](docs/api-contract.md)
- Event model: [docs/event-model.md](docs/event-model.md)
- Update ingestion contract (Sprint 2): [docs/update-ingestion.md](docs/update-ingestion.md)
- Roadmap: [docs/roadmap.md](docs/roadmap.md)
- Contributing workflow: [docs/contributing.md](docs/contributing.md)
- ADR index: [docs/adr/README.md](docs/adr/README.md)

## Source of truth

- MAX API docs: [https://dev.max.ru/docs-api](https://dev.max.ru/docs-api)
- MAX methods: [https://dev.max.ru/docs-api/methods](https://dev.max.ru/docs-api/methods)
- MAX objects: [https://dev.max.ru/docs-api/objects](https://dev.max.ru/docs-api/objects)
- aiogram 3 reference: [https://docs.aiogram.dev/](https://docs.aiogram.dev/)
