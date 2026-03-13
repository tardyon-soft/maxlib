# Testkit Contract (Sprint 9.1.1)

Документ фиксирует контракт слоя `max-testkit`.
Это спецификация test utilities, а не полноценный e2e harness.

## Goal

- дать быстрый и повторяемый способ тестировать runtime handlers/routers;
- покрывать сценарии без реального MAX API и без поднятия внешней инфраструктуры;
- переиспользовать существующие core contracts (`Dispatcher`, `Router`, `RuntimeContext`, ingestion APIs).

## Responsibilities

`max-testkit` отвечает за:
- test fixtures/builders для `Update`/`Message`/`Callback`;
- удобный запуск dispatch pipeline в тестах;
- assertions helpers для `DispatchResult`, enrichment/state transitions;
- lightweight doubles/stubs для integration-style сценариев.

`max-testkit` не отвечает за:
- замену unit-тестов framework модулей;
- full e2e against real MAX API;
- performance/load testing framework.

## Core Testkit Contracts

Минимальный ожидаемый surface:
- `UpdateFixtures` (или эквивалент): типизированные фабрики update-событий;
- `DispatcherTestHarness` (или эквивалент):
  - создаёт dispatcher с роутерами;
  - позволяет `feedUpdate(...)`;
  - возвращает удобный test result/trace;
- helpers для polling/webhook ingestion integration checks;
- helpers для FSM/scenes state assertions.

## Integration Boundaries

Testkit должен тестировать существующее ядро:
- routing и first-match semantics;
- filters/middleware/context enrichment;
- DI parameter resolution;
- FSM/scenes transitions;
- messaging/callback/action facades через mocks/stubs.

Testkit не должен дублировать runtime implementation.

## Desired DX

Пример handler-теста через harness:

```java
Router router = new Router("main");
router.message((message, fsm) ->
    fsm.setState("form.email").thenApply(v -> null)
);

// pseudo-API: итоговый surface может отличаться, но сценарий должен быть эквивалентен
DispatchResult result = harness(dispatcher -> dispatcher.includeRouter(router))
    .feed(updateMessage("hello"))
    .join();
```

Webhook/polling test story:
- создавать фикстуры payload/update;
- прогонять через `WebhookReceiver`/`LongPollingRunner` + `UpdateSink`;
- валидировать, что downstream dispatch отработал одинаково.

## Sprint 9 Boundaries

- не строим тяжёлый e2e suite;
- не строим universal mock server platform;
- не делаем framework-specific magic, скрывающий поведение runtime.
