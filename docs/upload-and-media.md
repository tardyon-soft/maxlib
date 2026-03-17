# Upload and Media Guide

## Minimal wiring

```java
Dispatcher dispatcher = new Dispatcher()
        .withBotClient(maxBotClient)
        .withUploadService(uploadService);
```

## Runtime usage

```java
router.message(BuiltInFilters.command("photo"), (message, ctx) -> {
    ctx.replyImage(InputFile.fromPath(Path.of("./sample.jpg")));
    return CompletableFuture.completedFuture(null);
});
```

## Lower-level usage

Можно работать напрямую через `UploadService` + `MediaMessagingFacade`, если нужен более явный контроль flow.

## Notes

- Upload flow асинхронный и может вернуть transport/API exceptions.
- Для webhook/polling поведение одинаково (единый dispatcher runtime).
