# Testkit

## Module

`max-testkit` даёт быстрый runtime test harness без ручного wiring.

## Main types

- `DispatcherTestKit`
- `ScreenTestKit`
- `ScreenFlowProbe`
- `UpdateFixtures` / `TestUpdates`
- `ScreenFixtures`
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
- active screen id / params (`ScreenFlowProbe.assertTopScreen/assertTopParam`)
- rendered callback payload in keyboard (`ScreenFlowProbe.assertLastRenderedPayload`)

## Screen flow example (без Spring)

```java
InMemoryScreenRegistry registry = new InMemoryScreenRegistry();
registry.register(new HomeScreen()).register(new ProfileScreen());

Router entryRouter = new Router("entry");
entryRouter.message(BuiltInFilters.command("screen"), (message, ctx) ->
        Screens.navigator(ctx, registry).start("home", Map.of())
);

ScreenTestKit kit = ScreenTestKit.builder()
        .registry(registry)
        .includeRouter(entryRouter)
        .build();

kit.feed(TestUpdates.message("u-1", "c-1", "/screen"))
        .assertLastHandled()
        .assertTopScreen("home")
        .assertLastRenderedPayload(kit.actionPayload("open_profile"));
```

Fixtures для screen-flow:

- `ScreenFixtures.actionPayload(...)` — callback payload без ручного кодирования.
- `ScreenFixtures.activeSession(...)` + `seedState(...)` — подготовка screen state в FSM.
