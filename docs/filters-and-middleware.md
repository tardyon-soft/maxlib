# Filters and Middleware

## Pipeline

Для одного update runtime pipeline выполняется в порядке:

1. outer middleware
2. filter evaluation
3. inner middleware
4. handler invocation

## Practical pattern

```java
Dispatcher dispatcher = new Dispatcher();
dispatcher.outerMiddleware((ctx, next) -> {
    ctx.putEnrichment("requestId", "req-1");
    return next.proceed();
});

Router router = new Router("main");
router.innerMiddleware((ctx, next) -> {
    ctx.putEnrichment("attempt", 1);
    return next.proceed();
});

router.message(BuiltInFilters.command("start"), (message, ctx) -> {
    ctx.reply(Messages.text("ok"));
    return CompletableFuture.completedFuture(null);
});
```

## Notes

- Filter enrichment merge выполняется до inner middleware.
- Conflicting enrichment values приводят к dispatch failure.
- Ошибки filter/middleware попадают в `router.error(...)` boundary.
