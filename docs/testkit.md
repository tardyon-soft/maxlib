# Testkit

## Module

`max-testkit` даёт быстрый runtime test harness без ручного wiring.

## Main types

- `DispatcherTestKit`
- `UpdateFixtures` / `TestUpdates`
- `RecordingMaxBotClient`
- `CapturedApiCall`

## Example

```java
Router router = new Router("test");
router.message((message, ctx) -> {
    ctx.reply(Messages.text("pong"));
    return CompletableFuture.completedFuture(null);
});

DispatcherTestKit kit = DispatcherTestKit.withRouter(router);
DispatcherTestKit.DispatchProbe probe = kit.feedAndCapture(UpdateFixtures.message().text("ping").build());
```

## Typical assertions

- dispatch status (`HANDLED/IGNORED/FAILED`)
- emitted API calls
- callback answers
- state transitions (при FSM wiring)
