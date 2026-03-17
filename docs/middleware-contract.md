# Middleware Contract

## Interface

```java
@FunctionalInterface
public interface Middleware {
    CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next);
}
```

`MiddlewareNext.proceed()` продолжает цепочку.

## Types

- `OuterMiddleware` — вокруг полного update dispatch.
- `InnerMiddleware` — вокруг matched handler execution.

## Semantics

- Middleware может short-circuit (вернуть результат без `next.proceed()`).
- Ошибки middleware оборачиваются в `MiddlewareExecutionException`.
- Inner middleware видит filter enrichment текущего matched route.

## Context usage

Middleware может:

- читать/писать enrichment (`context.putEnrichment(...)`)
- читать текущий `Update`
- использовать runtime shortcuts (если соответствующие сервисы wired)
