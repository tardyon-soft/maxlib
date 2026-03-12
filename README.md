# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX с DX в стиле aiogram 3.

Важно: это не буквальная копия aiogram 3. Проект адаптирует лучшие архитектурные идеи под реальные возможности MAX API и Java ecosystem.

## Sprint status

Текущий этап: `Sprint 3 — Dispatcher/Router foundation`.

Завершённые этапы:
- Sprint 1 (`client/DTO/errors`);
- Sprint 2 (`polling + webhook ingestion layer`).

Что уже реализовано:
- multi-module Gradle проект (Kotlin DSL) на Java 21;
- `max-client-core` foundation слой (transport, auth, serialization, errors, retry/rate-limit hooks, pagination);
- `max-model` с базовыми DTO, typed value objects и enum-контрактами;
- зафиксирован runtime contract Sprint 3 (`Dispatcher`, `Router`, `EventObserver`, `Handler`, `DispatchResult`);
- реализован базовый observer layer в `max-dispatcher`: `EventObserver`, `EventHandler`, `DefaultEventObserver`, MVP observer types (`update/message/callback/error`);
- ingestion target contract в `max-dispatcher`: `UpdateConsumer` (async, preferred) + `UpdateSink` (compat alias) + `UpdateHandlingResult`;
- polling source abstraction в `max-dispatcher`: `PollingUpdateSource` + `SdkPollingUpdateSource` (SDK-backed `getUpdates` pull);
- long polling runtime foundation: `DefaultLongPollingRunner` с lifecycle API (`start/stop/shutdown/isRunning`);
- graceful lifecycle semantics для polling runner: `stop()` vs `shutdown()` + ownership-aware resource cleanup;
- marker progression contract: monotonic marker state с продвижением только после успешного batch handling;
- webhook secret validation foundation: `WebhookSecretValidator` + typed validation result/error contracts;
- webhook receiver foundation: `DefaultWebhookReceiver` (`WebhookRequest` -> `WebhookReceiveResult`);
- webhook overload control foundation: `WebhookReceiverConfig.maxInFlightRequests` + `OVERLOADED` result;
- unified ingestion pipeline foundation: `UpdatePipeline` + `DefaultUpdatePipeline` + `UpdatePipelineContext`;
- integration-style ingestion tests with JSON fixtures for polling/webhook regression safety;
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

Ограничения текущего этапа (Sprint 3 prep):
- runtime Sprint 3 пока на уровне контракта: каркас `Dispatcher/Router` pipeline только начинается;
- Router пока предоставляет только базовые observer registries без полноценного routing runtime;
- filters полноценного уровня ещё не реализованы;
- middleware runtime chain ещё не реализован;
- DI runtime и FSM/scenes runtime ещё не реализованы;
- Spring Boot starter и testkit пока на уровне скелетов модулей;
- upload/media pipeline ещё не реализован;
- webhook source runtime loop пока не реализован (есть receiver + pipeline foundation);
- surface MAX API покрыт частично и будет расширяться в следующих спринтах.

## Sprint 2 Summary

- реализован transport-level ingestion для polling и webhook;
- polling и webhook сведены к единому `UpdatePipeline`;
- зафиксированы marker strategy, lifecycle/shutdown semantics и overload control;
- добавлены integration-style fixtures/tests как regression safety net перед Sprint 3.

## Low-level Long Polling Example

```java
import java.time.Duration;
import ru.max.botframework.ingestion.DefaultLongPollingRunner;
import ru.max.botframework.ingestion.LongPollingRunner;
import ru.max.botframework.ingestion.LongPollingRunnerConfig;
import ru.max.botframework.ingestion.PollingFetchRequest;
import ru.max.botframework.ingestion.SdkPollingUpdateSource;
import ru.max.botframework.ingestion.UpdateConsumer;
import ru.max.botframework.model.UpdateEventType;

SdkPollingUpdateSource source = new SdkPollingUpdateSource(botClient);
UpdateConsumer sink = update -> {
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
- контракт: `WebhookSecretValidator.validate(String secretHeader)`;
- outcomes:
- `ACCEPTED`
- `SKIPPED_NO_SECRET_CONFIGURED`
- `REJECTED` (`SECRET_HEADER_MISSING` или `SECRET_MISMATCH`).

## Unified Ingestion Pipeline

- polling runner и webhook receiver используют один transport-level contract: `UpdatePipeline`;
- стандартная реализация: `DefaultUpdatePipeline`, которая делегирует в `UpdateConsumer` (`UpdateSink` alias);
- контекст источника фиксируется через `UpdatePipelineContext` (`POLLING` / `WEBHOOK`);
- расширение для observability: `UpdatePipelineHook` (`onBefore` / `onAfter`).

## Lifecycle and Shutdown

- `DefaultLongPollingRunner.stop()` — graceful stop цикла polling без финального закрытия компонентов;
- `DefaultLongPollingRunner.shutdown()` — финальная остановка и cleanup ресурсов по `LongPollingRunnerConfig`;
- ownership lifecycle управляется конфигом:
- `closeExecutorOnShutdown`
- `closeSourceOnShutdown`
- `shutdownTimeout`

## Webhook Overload Control

- receiver использует `WebhookReceiverConfig(maxInFlightRequests)`;
- при достижении лимита возвращается `WebhookReceiveStatus.OVERLOADED`;
- это lightweight backpressure на transport-уровне без reactive engine.

## Ingestion Integration Tests

- polling chain coverage: `polling -> SdkPollingUpdateSource -> DefaultLongPollingRunner -> UpdateConsumer`;
- webhook chain coverage: `webhook request -> DefaultWebhookReceiver -> secret validation -> UpdateConsumer`;
- error coverage:
- webhook payload deserialization errors (`BAD_PAYLOAD`);
- sink failures (`INTERNAL_ERROR`);
- fixtures path: `max-dispatcher/src/test/resources/fixtures/ingestion`.

## Sprint 2 Examples

- low-level examples directory: `examples/sprint-2-low-level`;
- files:
- `LongPollingExample.java`
- `WebhookHandlingExample.java`

## Router Registration (Sprint 3 foundation)

```java
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.dispatcher.Router;

Router router = new Router("main")
    .update(update -> {
        System.out.println("Any update: " + update.updateId().value());
        return CompletableFuture.completedFuture(null);
    })
    .message(message -> {
        System.out.println("Message text: " + message.text());
        return CompletableFuture.completedFuture(null);
    })
    .callback(callback -> CompletableFuture.completedFuture(null))
    .error(error -> {
        error.error().printStackTrace();
        return CompletableFuture.completedFuture(null);
    });

Router admin = new Router("admin")
    .message(message -> CompletableFuture.completedFuture(null));

router.includeRouter(admin);
```

## Dispatcher Role (Sprint 3 foundation)

`Dispatcher` — корневой runtime orchestrator над root routers.

- хранит root routing graph (`includeRouter`, `includeRouters`);
- даёт единую dispatch entrypoint: `feedUpdate(Update) -> DispatchResult`;
- реализует ingestion boundary `UpdateConsumer` через `handle(Update)`.

```java
Dispatcher dispatcher = new Dispatcher()
    .includeRouter(router);

DispatchResult result = dispatcher.feedUpdate(update).toCompletableFuture().join();
```

Event mapping strategy:
- всегда вызывается generic `update` observer;
- затем `UpdateEventResolver` маппит update в `message`/`callback`/`unsupported`;
- fallback: если type не распознан, но payload содержит `message` или `callback`, используется соответствующий observer;
- `unsupported` update без подходящего payload даёт `DispatchResult.IGNORED`.

## Low-level Webhook Handling Example

```java
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.ingestion.DefaultWebhookReceiver;
import ru.max.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.max.botframework.ingestion.UpdateHandlingResult;
import ru.max.botframework.ingestion.UpdateConsumer;
import ru.max.botframework.ingestion.WebhookReceiveResult;
import ru.max.botframework.ingestion.WebhookReceiveStatus;
import ru.max.botframework.ingestion.WebhookRequest;

UpdateConsumer sink = update -> CompletableFuture.completedFuture(UpdateHandlingResult.success());

DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
    new DefaultWebhookSecretValidator("my-secret"),
    new JacksonJsonCodec(),
    sink
);

WebhookRequest request = new WebhookRequest(
    rawBodyBytes,
    Map.of("X-Max-Bot-Api-Secret", List.of(secretFromHttpHeader))
);

WebhookReceiveResult result = receiver.receive(request).toCompletableFuture().join();
if (result.status() == WebhookReceiveStatus.ACCEPTED) {
    // return HTTP 200
} else if (result.status() == WebhookReceiveStatus.INVALID_SECRET) {
    // return HTTP 401/403
} else if (result.status() == WebhookReceiveStatus.BAD_PAYLOAD) {
    // return HTTP 400
} else if (result.status() == WebhookReceiveStatus.OVERLOADED) {
    // return HTTP 429/503
} else {
    // return HTTP 500
}
```

## Build and test

Требование: JDK 21+

```bash
./gradlew clean test
```

## Documentation

- Product vision and target DX: [docs/product-spec.md](docs/product-spec.md)
- Core API contract: [docs/api-contract.md](docs/api-contract.md)
- Runtime contract (Sprint 3): [docs/runtime-contract.md](docs/runtime-contract.md)
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
