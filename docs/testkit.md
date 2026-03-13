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

Реализованный baseline surface:
- `DispatcherTestKit`:
  - builder для runtime setup (`includeRouter`, `fsmStorage`, `stateScope`, scene/upload wiring);
  - `feed(Update)`, `feedAll(...)` и `handle(Update)` поверх реального `Dispatcher`;
  - `feedAndCapture(Update)` для side effects trace за один dispatch;
  - shortcut `DispatcherTestKit.withRouter(router)` для most-common тестов.
- `RecordingMaxBotClient`:
  - записывает все executed `MaxRequest`;
  - даёт deterministic default responses для базовых runtime операций;
  - поддерживает explicit response overrides (`respondWith(...)`).
- `CapturedApiCall`:
  - immutable snapshot запроса (`method/path/query/body/request`).
- `UpdateFixtures`:
  - builder fixtures для `message`/`callback` updates;
  - `statefulMessages(...)` helper для последовательного stateful flow в одном scope.
- `TestUpdates`:
  - backward-compatible shortcuts поверх `UpdateFixtures`.

## Integration Boundaries

Testkit должен тестировать существующее ядро:
- routing и first-match semantics;
- filters/middleware/context enrichment;
- DI parameter resolution;
- FSM/scenes transitions;
- messaging/callback/action facades через mocks/stubs.

Testkit не должен дублировать runtime implementation.

## Desired DX

Пример handler-теста через testkit:

```java
Router router = new Router("main");
router.message((message, context) -> {
    context.reply(Messages.text("pong"));
    return CompletableFuture.completedFuture(null);
});

DispatcherTestKit kit = DispatcherTestKit.builder()
    .includeRouter(router)
    .build();

DispatcherTestKit.DispatchProbe probe = kit.feedAndCapture(TestUpdates.message("ping"));
assertEquals(DispatchStatus.HANDLED, probe.result().status());
assertEquals(1, probe.sideEffects().size());
```

Stateful flow helper:

```java
var flow = UpdateFixtures.statefulMessages("user-1", "chat-1", "start", "email", "confirm");
var results = kit.feedAll(flow);
assertEquals(3, results.size());
```

In-repo usage example:
- `max-dispatcher/src/test/java/ru/max/botframework/dispatcher/DispatcherTestKitUsageExampleTest.java`
  показывает runtime handler test + side effects capture через `DispatcherTestKit`.

## Sprint 9 Boundaries

- не строим тяжёлый e2e suite;
- не строим universal mock server platform;
- не делаем framework-specific magic, скрывающий поведение runtime.

Статус обновлён (Sprint 9.4.3):
- testkit API финализирован как stable baseline для runtime/handler tests;
- naming и documentation выровнены с текущим публичным surface.
