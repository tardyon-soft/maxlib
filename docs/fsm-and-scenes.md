# FSM and Scenes Guide

## Quick wiring

```java
Dispatcher dispatcher = new Dispatcher()
        .withFsmStorage(new MemoryStorage())
        .withStateScope(StateScope.USER_IN_CHAT)
        .withSceneRegistry(new InMemorySceneRegistry())
        .withSceneStorage(new MemorySceneStorage());
```

## State flow example

```java
router.message(BuiltInFilters.command("form"), (message, ctx) ->
        ctx.fsm().setState("form.name")
                .thenAccept(ignored -> ctx.reply(Messages.text("Введите имя")))
);

router.message(BuiltInFilters.state("form.name"), (message, ctx) ->
        ctx.fsm().updateData(Map.of("name", message.text()))
                .thenCompose(updated -> ctx.fsm().setState("form.done"))
                .thenAccept(ignored -> ctx.reply(Messages.text("Сохранено")))
);
```

## Scene usage

```java
router.message(BuiltInFilters.command("scene-enter"), (message, ctx) ->
        ctx.scenes().enter("checkout")
);

router.message(BuiltInFilters.command("scene-exit"), (message, ctx) ->
        ctx.scenes().exit()
);
```

## Wizard usage

```java
router.message(BuiltInFilters.command("wizard-start"), (message, ctx) ->
        ctx.wizard().enter("onboarding")
);

router.message(BuiltInFilters.command("wizard-next"), (message, ctx) ->
        ctx.wizard().next()
);
```

## Notes

- FSM/scenes/wizard API асинхронный (`CompletionStage`).
- State filters используют текущий runtime scope.
- Без configured storage/scene runtime методы выбрасывают `IllegalStateException`.
