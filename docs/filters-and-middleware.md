# Filters, Middleware and Context Enrichment Contract (Sprint 4)

## Status

Документ фиксирует контракты Sprint 4 для:
- filter layer;
- middleware layer;
- runtime context enrichment.

Это спецификация публичного поведения, а не полная реализация.

Текущее состояние (Sprint 4.3.1):
- filter foundation реализован в `max-dispatcher`;
- middleware contracts и chain executor foundation реализованы;
- middleware встроены в dispatcher runtime pipeline:
  - outer middleware на уровне `Dispatcher`;
  - inner middleware на уровне `Router` вокруг matched handler execution.
- runtime enrichment model закреплён:
  - middleware enrichment через `RuntimeContext.putEnrichment(...)`;
  - filter enrichment merge через runtime pipeline;
  - key conflict policy: conflicting values -> `EnrichmentConflictException`.

## Sprint 4 boundaries

В scope Sprint 4:
- базовый filter DSL и filter evaluation в runtime pipeline;
- outer/inner middleware contract и порядок выполнения;
- request-scoped context enrichment для filters/middleware/handlers.

Вне scope Sprint 4:
- полноценный DI parameter resolution pipeline;
- FSM/scenes/state runtime;
- сложная middleware bus архитектура и distributed orchestration.

## Core contracts

### `Filter`

Назначение:
- проверить, подходит ли событие для handler-а;
- при успехе экспортировать typed bindings в runtime context.

Контракт (концепт):

```java
@FunctionalInterface
public interface Filter<C> {
    CompletionStage<FilterResult> test(C context);
}
```

Требования:
- фильтр не должен знать transport детали polling/webhook;
- фильтр должен быть детерминированным относительно входного runtime context;
- тяжёлые I/O операции в фильтрах не являются целевым сценарием Sprint 4.

### `FilterResult`

Назначение:
- явный результат filter evaluation без исключений как control-flow.

MVP outcomes:
- `MATCHED` (+ optional filter-produced bindings);
- `NOT_MATCHED`;
- `FAILED` (ошибка фильтра как runtime failure).

### Built-in filters (MVP baseline)

MVP baseline Sprint 4:
- message text presence;
- message text equals/startsWith;
- callback data presence;
- callback data equals/startsWith;
- any/update-type filters.

Цель Sprint 4:
- дать минимально полезный набор без полного aiogram DSL surface.

### Filter composition

Поддерживаемые операции:
- `and`;
- `or`;
- `not`.

Правила:
- left-to-right evaluation;
- short-circuit для `and`/`or`;
- deterministic merge filter bindings;
- при конфликте bindings (одинаковый ключ, несовместимые значения) результат — `NOT_MATCHED` (fail-safe).

## Middleware contracts

### `OuterMiddleware`

Назначение:
- оборачивает весь dispatch pipeline одного update;
- работает до filter evaluation и после завершения handler stage.

Типичные use-cases:
- logging;
- tracing/correlation id;
- global metrics;
- global error wrapping.

### `InnerMiddleware`

Назначение:
- оборачивает только execution matched handler;
- запускается после filter stage.

Типичные use-cases:
- route-level permissions;
- route-level metrics;
- post-filter enrichment.

## Runtime pipeline order (Sprint 4 target)

Для одного update порядок выполнения:

1. Outer middleware chain (pre).
2. Event mapping + router traversal (из Sprint 3 runtime).
3. Filter evaluation для candidate handlers.
4. Inner middleware chain (pre).
5. Handler execution.
6. Inner middleware chain (post/unwind).
7. Outer middleware chain (post/unwind).

First-match remains:
- как только найден и успешно выполнен первый matched handler, дальнейший поиск прекращается.

## Context enrichment contract

### Enrichment sources

В request-scoped runtime context могут добавляться данные из:
- filters (filter-produced bindings);
- outer middleware;
- inner middleware;
- framework runtime metadata (update id/type/source).

### Visibility rules

- данные outer middleware доступны filters, inner middleware и handler;
- данные filter stage доступны inner middleware и handler matched route;
- данные inner middleware доступны handler и post-stage текущего route pipeline;
- enrichment не должен выходить за границы одного update processing.

### Data model

MVP contract:
- typed key-value map (`ContextKey<T>`/эквивалент) с безопасным доступом;
- string-key enrichment namespace для filter-produced bindings;
- typed read API для enrichment (`enrichmentValue(key, type)` / `enrichmentValue(ContextKey<T>)`);
- conflict policy: одинаковый ключ с разными значениями -> runtime failure;
- no global mutable singleton state;
- no implicit persistence.

## Error boundaries

Разделение ошибок:
- ingestion errors (transport/parsing/webhook validation) остаются в ingestion layer;
- runtime errors (filters/middleware/handler) остаются в dispatcher/runtime layer.

Обработка ошибок в Sprint 4:
- filter failure -> runtime `FAILED` + error observer (`FILTER_FAILURE`);
- outer middleware failure -> runtime `FAILED` + error observer (`OUTER_MIDDLEWARE_FAILURE`);
- inner middleware failure -> runtime `FAILED` + error observer (`INNER_MIDDLEWARE_FAILURE`);
- enrichment merge conflict -> runtime `FAILED` + error observer (`ENRICHMENT_FAILURE`);
- handler failure -> runtime `FAILED` + error observer (уже в Sprint 3).

## Design constraints

- Не вводить тяжёлый DI container в Sprint 4.
- Не смешивать filter/middleware API с transport abstractions.
- Сохранить расширяемость к Sprint 5 (DI) без ломающего redesign публичных сигнатур.

## Test coverage (Sprint 4.3.2)

- unit tests: `Filter`, `FilterResult` composition rules, built-in filters, middleware chain contracts,
  runtime context enrichment, runtime error behavior;
- integration-style tests: `Dispatcher + Router + Filters`, `Dispatcher + Router + Middleware`,
  combined pipeline order and enrichment visibility in router tree flow.

## Related docs

- [runtime-contract.md](runtime-contract.md)
- [api-contract.md](api-contract.md)
- [filter-contract.md](filter-contract.md)
- [middleware-contract.md](middleware-contract.md)
- [di-model.md](di-model.md)
