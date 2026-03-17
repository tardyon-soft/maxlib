# Message API Contract

## Core abstractions

- `MessageBuilder` / `Messages`
- `MessagingFacade`
- `MessageTarget`
- keyboard DSL: `Buttons`, `Keyboards`, `KeyboardBuilder`

## Guarantees

- Все message operations идут через `MaxBotClient`.
- Builder-API transport-agnostic в рамках runtime.
- Reply helpers берут chat/message context из текущего update.

## Runtime helpers

В `RuntimeContext`:

- `reply(MessageBuilder)`
- `messaging()`

## Typical usage

```java
router.message(BuiltInFilters.command("start"), (message, ctx) -> {
    ctx.reply(
            Messages.text("Выберите действие")
                    .keyboard(Keyboards.inline(k -> k.row(
                            Buttons.callback("Оплатить", "menu:pay")
                    )))
    );
    return CompletableFuture.completedFuture(null);
});
```
