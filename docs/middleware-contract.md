# Middleware Contract Specification

## Status

Документ фиксирует middleware contract для framework MVP.
Это спецификация публичного поведения, а не реализация.

## Goals

- Определить роли `outer middleware` и `inner middleware`.
- Зафиксировать детерминированный порядок выполнения.
- Описать правила доступа к `Context` и enrichment данных.
- Зафиксировать связь middleware pipeline с error handling.

## Middleware types

### Outer middleware

Назначение:
- оборачивает весь dispatcher pipeline для одного update;
- выполняется до router resolution и после завершения обработки;
- используется для logging, tracing, metrics, global auth/rate limits, correlation id.

Область действия:
- весь update processing (routing, filters, inner middleware, handler).

### Inner middleware

Назначение:
- оборачивает выполнение конкретного matched route handler;
- выполняется после выбора handler (после filters);
- используется для route-scoped concerns: permission checks, scene/session guards, route-local metrics.

Область действия:
- только matched route execution.

## Public contract (concept)

```java
@FunctionalInterface
public interface Middleware<C extends Context> {
    CompletionStage<Void> invoke(C ctx, Next next);
}

@FunctionalInterface
public interface Next {
    CompletionStage<Void> invoke();
}
```

Контракт вызова:
- middleware обязан либо вызвать `next.invoke()`, либо завершить pipeline самостоятельно;
- middleware может выполнить pre/post логику вокруг `next.invoke()`;
- middleware должен быть безопасным для многопоточной обработки разных update.

## Execution order

Для одного update порядок фиксирован:

1. `Dispatcher outer middleware` (в порядке регистрации, first-in outermost).
2. `Router resolution` + observer filter evaluation.
3. `Matched router inner middleware` (в порядке регистрации, first-in outermost).
4. `Handler`.
5. unwind inner middleware (post section).
6. unwind outer middleware (post section).

Следствие:
- outer middleware видит полный end-to-end latency update;
- inner middleware видит latency конкретного handler execution.

## Context access contract

В обоих типах middleware доступно:
- `Context` текущего update;
- normalized update meta (`updateId`, `updateType`, source);
- request-scoped attributes API (enrichment storage).

Ограничения:
- middleware не должен модифицировать immutable update payload объекты;
- долгоживущие данные не должны храниться в request-scoped context.

## Data enrichment contract

### Request-scoped enrichment

Middleware может обогащать context через typed attributes.

```java
ContextKey<String> CORRELATION_ID = ContextKey.of("correlationId", String.class);

dispatcher.outerMiddleware((ctx, next) -> {
    ctx.attributes().put(CORRELATION_ID, UUID.randomUUID().toString());
    return next.invoke();
});
```

Правила:
- enrichment живёт только в рамках одного update;
- outer enrichment доступен inner middleware и handler;
- inner enrichment доступен только текущему matched route pipeline;
- конфликт ключей решается last-write-wins в пределах одного pipeline шага.

### Relation to DI

Значения из context attributes могут резолвиться в handler параметры (post-MVP расширение), но для MVP гарантируется доступ через `ctx.attributes()` API.

## Error handling contract

### Propagation model

- Исключение в handler сначала поднимается в inner middleware unwind.
- Если inner middleware не обработал исключение, оно поднимается в outer middleware unwind.
- Если outer middleware не обработал исключение, исключение попадает в dispatcher error handler.

### Handling rules

- Middleware может перехватить исключение и:
  1. rethrow (продолжить propagation),
  2. map в framework exception,
  3. завершить pipeline fallback-ответом.
- Глотать исключения без логирования запрещено контрактом observability.
- `Dispatcher.onError(...)` является final safety net.

### Unknown/Unhandled updates

- Если handler не найден, middleware pipeline всё равно считается успешно выполненным (без handler stage).
- outer middleware получает возможность логировать факт unhandled update.

## Example

```java
dispatcher
    .outerMiddleware((ctx, next) -> {
        long started = System.nanoTime();
        try {
            return next.invoke();
        } finally {
            metrics.record("update.latency", System.nanoTime() - started, ctx.updateType());
        }
    })
    .onError((ctx, ex) -> {
        logger.error("Unhandled update error", ex);
        return ctx.reply("Internal error");
    });

router.innerMiddleware((ctx, next) -> {
    if (!permissions.canHandle(ctx.user())) {
        return ctx.reply("Access denied");
    }
    return next.invoke();
});
```

## Non-goals

- Описание internal threading/concurrency implementation.
- Детали конкретной telemetry backend интеграции.
- Полный набор built-in middleware реализаций.

## Related docs

- [api-contract.md](api-contract.md)
- [filter-contract.md](filter-contract.md)
- [event-model.md](event-model.md)
- [adr/0001-router-model.md](adr/0001-router-model.md)
