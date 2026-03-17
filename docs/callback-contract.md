# Callback Contract

## Event type

Callback update представлен `ru.tardyon.botframework.model.Callback` внутри `Update`.

## Registration API

```java
Router router = new Router("main");
router.callback((callback, ctx) -> {
    ctx.answerCallback("OK");
    return CompletableFuture.completedFuture(null);
});
```

С фильтром:

```java
router.callback(BuiltInFilters.callbackDataStartsWith("menu:"), (callback, ctx) -> {
    ctx.answerCallback("menu action");
    return CompletableFuture.completedFuture(null);
});
```

## Runtime helpers

Через `RuntimeContext` доступны:

- `answerCallback(String)`
- `callbacks()` (`CallbackFacade`)
- `reply(...)`/`messaging()` для реакций сообщением

## Built-in callback filters

- `callbackDataPresent()`
- `callbackDataEquals(value)`
- `callbackDataStartsWith(prefix)`
- `fromCallbackUser(userId)`

## Annotation sugar

Для callback handlers доступны:

- `@Callback("menu:pay")`
- `@CallbackPrefix("menu:")`

Они транслируются в обычные callback filters через `AnnotatedRouteRegistrar`.
