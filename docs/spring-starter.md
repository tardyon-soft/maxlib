# Spring Boot Starter Contract (Sprint 9.1.1)

Документ фиксирует контракт слоя `max-spring-boot-starter`.
Это спецификация интеграции, а не полная реализация.

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
- `max.bot.polling.limit` (`int`);
- `max.bot.polling.timeout` (`Duration`);
- `max.bot.polling.types` (`List<UpdateType>`, optional);
- `max.bot.webhook.enabled` (`boolean`);
- `max.bot.webhook.path` (`String`);
- `max.bot.webhook.secret` (`String`, optional but recommended);
- `max.bot.webhook.max-in-flight` (`int`, optional).

Примечание: итоговые имена полей могут эволюционировать, но scope и семантика должны остаться.

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

Допустимый минимум для Sprint 9:
- explicit bean registration без тяжёлого classpath scanning framework.

## Desired DX

Минимальное Spring Boot приложение:

```java
@Configuration
class BotConfig {
    @Bean
    Router mainRouter() {
        Router router = new Router("main");
        router.message(message -> java.util.concurrent.CompletableFuture.completedFuture(null));
        return router;
    }
}
```

Polling mode:
- достаточно `max.bot.mode=POLLING` и `Router` bean-ов.

Webhook mode:
- `max.bot.mode=WEBHOOK` + `max.bot.webhook.path` + `max.bot.webhook.secret`.

## Sprint 9 Boundaries

- не делаем giant enterprise platform;
- не делаем полный observability suite;
- не делаем cloud deployment toolkit;
- не делаем второй runtime вне существующего dispatcher/ingestion ядра.
