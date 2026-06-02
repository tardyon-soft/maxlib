# Dispatcher, Router, filters и middleware

## Dispatcher

`Dispatcher` - корневой runtime-объект. Он:

- принимает `Update`;
- определяет тип события;
- вызывает `Router`;
- дает `RuntimeContext`;
- подключает FSM, scene, screen и upload runtime;
- может работать с polling и webhook ingestion API.

Базовая сборка:

```java
Dispatcher dispatcher = new Dispatcher()
        .withBotClient(botClient)
        .withFsmStorage(new MemoryStorage())
        .includeRouter(router);
```

## Router

`Router` регистрирует обработчики для:

- `update(...)`
- `message(...)`
- `callback(...)`
- `error(...)`

Есть два стиля:

1. Классический API через `Router`.
2. Аннотационный API через `@Route` и `AnnotatedRouteRegistrar`.

## Встроенные фильтры

В `BuiltInFilters` есть готовые матчи:

- `command(...)`
- `textEquals(...)`
- `textStartsWith(...)`
- `chatType(...)`
- `fromUser(...)`
- `fromCallbackUser(...)`
- `hasAttachment()`
- `callbackDataPresent()`
- `callbackDataEquals(...)`
- `callbackDataStartsWith(...)`
- `state(...)`
- `stateIn(...)`

Пример:

```java
router.message(BuiltInFilters.command("start"), (message, ctx) -> {
    ctx.reply(Messages.text("Старт"));
    return CompletableFuture.completedFuture(null);
});
```

## Middleware

Есть два уровня middleware:

- `OuterMiddleware` - на уровне `Dispatcher`
- `InnerMiddleware` - на уровне `Router` или конкретного annotated-handler

`RuntimeContext` позволяет передавать enrichment-данные дальше по цепочке:

```java
public final class AttemptMiddleware implements InnerMiddleware {
    @Override
    public CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next) {
        context.putEnrichment("attempt", 1);
        return next.proceed();
    }
}
```

## RuntimeContext

В обработчиках через `RuntimeContext` доступны:

- `messaging()`
- `callbacks()`
- `actions()`
- `media()`
- `fsm()`
- `fsm(namespace)`
- `scenes()`
- `wizard()`
- `reply(...)`
- `answerCallback(...)`
- `chatAction(...)`

Если соответствующий runtime не сконфигурирован, вызов завершится `IllegalStateException`.

## Аннотационный API

Поддерживаются аннотации:

- `@Route`
- `@Message`
- `@Text`
- `@Command`
- `@Callback`
- `@CallbackPrefix`
- `@State`
- `@UseFilters`
- `@UseMiddleware`

Минимальный пример:

```java
@Route("menu")
public final class MenuRoute {
    @Command("menu")
    public void menu(RuntimeContext ctx) {
        ctx.reply(Messages.text("Меню"));
    }
}
```
