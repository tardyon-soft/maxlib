# Spring Boot Starter Contract (Sprint 9.1.1)

Документ фиксирует контракт слоя `max-spring-boot-starter`.
Это спецификация интеграции, а не полная реализация.

Текущий статус (Sprint 9.1.2):
- создан starter module foundation (`properties`, `autoconfigure`, `polling`, `webhook`);
- добавлен `AutoConfiguration.imports` entrypoint;
- полноценный runtime wiring и lifecycle orchestration будут добавляться последующими задачами Sprint 9.

Статус обновлён (Sprint 9.2.1):
- autoconfiguration поднимает core SDK/runtime beans:
  `MaxApiClientConfig`, `MaxHttpClient`, `MaxBotClient`, `FSMStorage`, `Dispatcher`;
- starter включает `Router` beans в root `Dispatcher` и применяет `storage.state-scope`;
- polling/webhook lifecycle wiring остаётся отдельной задачей.

Статус обновлён (Sprint 9.2.2):
- реализован Spring webhook adapter path:
  - `WebhookSecretValidator` + `WebhookReceiverConfig` + `DefaultWebhookReceiver` автоконфигурируются;
  - Spring MVC endpoint (`POST ${max.bot.webhook.path}`) делегирует в framework-agnostic receiver;
  - response mapping: `200/403/400/429/500`.

Статус обновлён (Sprint 9.2.3):
- реализован polling bootstrap:
  - `PollingUpdateSource` (`SdkPollingUpdateSource`) + `LongPollingRunnerConfig` + `DefaultLongPollingRunner`;
  - `SpringPollingLifecycle` запускает/останавливает polling вместе с lifecycle приложения.

Статус обновлён (Sprint 9.3.1):
- starter auto-configures runtime-facing services:
  - `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`;
  - `MediaMessagingFacade` при наличии `UploadService`;
- starter auto-configures FSM/scene defaults:
  - `FSMStorage` -> `MemoryStorage`,
  - `SceneRegistry` -> `InMemorySceneRegistry`,
  - `SceneStorage` -> `MemorySceneStorage`.

## Goal

- дать быстрый bootstrapping framework в Spring Boot приложении;
- переиспользовать существующее ядро (`max-client-core`, `max-dispatcher`, `max-fsm`);
- не дублировать runtime/transport логику в starter.

## Responsibilities

`max-spring-boot-starter` отвечает за:
- configuration properties binding;
- autoconfiguration базовых bean-ов;
- wiring polling/webhook bootstrap поверх существующих ingestion/runtime абстракций;
- bridge для регистрации router/handler graph из Spring-контекста.

`max-spring-boot-starter` не отвечает за:
- новый dispatcher/runtime;
- новый SDK client transport;
- enterprise deployment/orchestration framework.

## Configuration Properties (contract)

`max.bot.*`:
- `max.bot.token` (`String`, required);
- `max.bot.base-url` (`String`, optional, default `https://api.max.ru`);
- `max.bot.mode` (`POLLING` | `WEBHOOK`, default `POLLING`);
- `max.bot.polling.enabled` (`boolean`);
- `max.bot.polling.limit` (`int`, default `100`);
- `max.bot.polling.timeout` (`Duration`, default `30s`);
- `max.bot.polling.types` (`List<UpdateEventType>`, optional; например `message_created`, `message_callback`);
- `max.bot.webhook.enabled` (`boolean`);
- `max.bot.webhook.path` (`String`, default `/webhook/max`);
- `max.bot.webhook.secret` (`String`, optional but recommended);
- `max.bot.webhook.max-in-flight` (`int`, optional).
- `max.bot.storage.type` (`MEMORY`, default `MEMORY`);
- `max.bot.storage.state-scope` (`USER` | `CHAT` | `USER_IN_CHAT`, default `USER_IN_CHAT`).

Примечание: итоговые имена полей могут эволюционировать, но scope и семантика должны остаться.

Validation baseline:
- `token` обязателен;
- `base-url` и `webhook.path` не должны быть пустыми;
- `polling.limit` и `webhook.max-in-flight` (если заданы) должны быть `> 0`.

## Autoconfiguration Contract

Starter должен автоконфигурировать (если не переопределено пользователем):
- `MaxApiClientConfig`;
- `MaxBotClient`;
- `Dispatcher`;
- `WebhookSecretValidator`;
- `WebhookReceiver`;
- `PollingUpdateSource` и `LongPollingRunner` (только в polling mode).

`@ConditionalOnMissingBean` используется как основной механизм расширяемости.

## Polling Bootstrap Contract

В режиме polling starter:
- создаёт `PollingUpdateSource` на основе existing SDK `getUpdates`;
- создаёт `LongPollingRunner`;
- подключает `Dispatcher` как `UpdateConsumer` (directly или adapter);
- управляет lifecycle (`start` при app ready, `stop/shutdown` при context close).

## Webhook Adapter Contract

Starter предоставляет thin adapter для Spring Web:
- принимает HTTP request (`headers + body`);
- делегирует в framework-agnostic `WebhookReceiver`;
- возвращает HTTP status на основе `WebhookReceiveResult`:
  - accepted/success -> `200/202`,
  - invalid secret -> `401/403`,
  - bad payload -> `400`,
  - internal error -> `500`.

Важно: adapter не должен содержать бизнес-логики dispatch.

## Router/Handler Registration Story

Базовый контракт:
- пользователь объявляет один или несколько `Router` bean-ов;
- starter включает их в `Dispatcher` в deterministic порядке;
- root `Dispatcher` bean остаётся единым entrypoint runtime.
- deterministic порядок задаётся через Spring ordering (`@Order` / `Ordered`);
- composition нескольких feature-router-ов делается обычным `dispatcher.includeRouter(...)`
  на стороне starter и/или через `router.includeRouter(child)` внутри router bean.

Допустимый минимум для Sprint 9:
- explicit bean registration без тяжёлого classpath scanning framework.
- без отдельного annotation ecosystem для handler discovery.

## Desired DX

Минимальное Spring Boot приложение:

```java
@Configuration
class BotConfig {
    @Bean
    @org.springframework.core.annotation.Order(10)
    Router mainRouter() {
        Router router = new Router("main");
        router.message(message -> java.util.concurrent.CompletableFuture.completedFuture(null));
        return router;
    }

    @Bean
    @org.springframework.core.annotation.Order(20)
    Router adminRouter() {
        Router router = new Router("admin");
        router.callback(callback -> java.util.concurrent.CompletableFuture.completedFuture(null));
        return router;
    }
}
```

В этом примере starter автоматически агрегирует оба router bean-а в `Dispatcher`
в порядке `mainRouter -> adminRouter`.

## Default Beans and Overrides

Default runtime beans:
- `MaxBotClient`, `Dispatcher`;
- `FSMStorage`, `SceneRegistry`, `SceneStorage`;
- `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`;
- `MediaMessagingFacade` (optional, only if `UploadService` exists).

Override pattern:

```java
@Configuration
class CustomStorageConfig {
    @Bean
    FSMStorage customStorage() {
        return new MemoryStorage(); // replace with custom impl later (Redis/SQL)
    }
}
```

Starter uses `@ConditionalOnMissingBean`, so user beans override defaults.

Polling mode:
- достаточно `max.bot.mode=POLLING` и `Router` bean-ов.
- polling lifecycle:
  - auto-start на startup;
  - graceful stop на shutdown.

Webhook mode:
- `max.bot.mode=WEBHOOK` + `max.bot.webhook.path` + `max.bot.webhook.secret`.
- endpoint example:

```http
POST /webhook/max
X-Max-Bot-Api-Secret: <configured-secret>
Content-Type: application/json
```

## Sprint 9 Boundaries

- не делаем giant enterprise platform;
- не делаем полный observability suite;
- не делаем cloud deployment toolkit;
- не делаем второй runtime вне существующего dispatcher/ingestion ядра.
