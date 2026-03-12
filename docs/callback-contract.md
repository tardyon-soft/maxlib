# Callback Contract Specification

## Status

Документ фиксирует callback contract framework для MVP.
Это спецификация публичного API и callback lifecycle, а не реализация.

## Goals

- Зафиксировать единую модель callback event.
- Определить handler API для callback observer.
- Описать abstraction для ответа на callback.
- Зафиксировать lifecycle callback flow end-to-end.

## Callback event model

Callback event — это normalized update с `UpdateType.CALLBACK`, содержащий:
- callback payload (`data`);
- метаданные источника (user/chat/message reference, где доступны);
- `UpdateMeta` (update id, transport source, timestamps).

Concept API:

```java
public interface CallbackContext extends Context {
    Callback callback();
    String callbackData();
    AnswerCallbackBuilder answerCallback(String text);
}
```

Требования:
- `callbackData()` возвращает raw payload;
- structured parsing выполняется filters layer (`F.callback().data().pattern(...)`);
- callback context живёт только в рамках одного update processing.

## Callback handler API

Базовый observer API:

```java
router.callback(Filter<CallbackContext> filter, CallbackHandler handler);
```

Handler формы (MVP):
- sync: `(CallbackContext ctx) -> { ... }`;
- async: `(CallbackContext ctx) -> CompletionStage<?>`;
- DI-enabled signatures с framework/app dependencies.

Примеры:

```java
router.callback(
    F.callback().data().startsWith("pay:"),
    ctx -> ctx.answerCallback("Оплата принята")
);

router.callback(
    F.callback().data().equalsTo("order:cancel"),
    (CallbackContext ctx, OrderService orders) -> {
        orders.cancelCurrent(ctx.userId());
        return ctx.answerCallback("Заказ отменён");
    }
);
```

## Callback answer abstraction

Ответ на callback оформляется отдельным builder API, не смешанным с message send API.

Concept API:

```java
AnswerCallbackBuilder answer = Callbacks.answer()
    .callbackId(ctx.callback().id())
    .text("Готово")
    .notify(true)
    .cacheSeconds(0);

ctx.answerCallback(answer);
```

Упрощённый API:

```java
ctx.answerCallback("OK");
```

Контракт:
- answer привязан к текущему callback event;
- `callbackId` может быть установлен автоматически из context;
- builder валидирует ограничения платформы до отправки;
- ошибки ответа типизируются как callback-operation errors.

## Lifecycle callback flow

Для одного callback update flow выглядит так:

1. Transport (polling/webhook) получает raw callback update.
2. Normalizer строит `UpdateType.CALLBACK`.
3. Dispatcher запускает outer middleware.
4. Router resolver выбирает callback observer.
5. Filters callback observer выполняются по порядку.
6. Для matched route выполняется inner middleware.
7. Callback handler вызывается с DI.
8. Handler может:
- отправить callback answer;
- отправить/изменить сообщение через Message API;
- изменить FSM state.
9. Исключения поднимаются по middleware цепочке в dispatcher error handler.
10. Update завершается transport acknowledgment.

## Success/error semantics

### Success

- Callback считается обработанным, если handler pipeline завершился без необработанных исключений.
- Callback answer может быть отправлен 0..1 раз в MVP рамках одного handler flow.

### Errors

- Если answer невалиден, возвращается `ValidationException`.
- Если MAX API вернул ошибку callback answer, возвращается `ApiRequestException`.
- Если handler выбрасывает исключение, применяется общий error contract dispatcher/middleware.

## Interaction with filters and DI

- Filter-produced data (например, parsed callback payload) доступна в handler injection через `@FromFilter`.
- Middleware attributes доступны через `@FromContext`.
- `CallbackContext` всегда приоритетный источник для callback-specific параметров.

Пример:

```java
router.callback(
    F.callback().data().pattern("order:(\\d+):pay", ORDER_ID, Long::parseLong),
    (CallbackContext ctx,
     @FromFilter(ORDER_ID) Long orderId,
     @FromContext(CORRELATION_ID) String correlationId,
     PaymentService payments) -> {
        payments.pay(orderId, correlationId);
        return ctx.answerCallback("Оплата подтверждена");
    }
);
```

## Non-goals

- Полный каталог всех callback answer опций MAX API.
- Реализация client retry/backoff политики для callback answer.
- UX детали конкретных клиентов MAX.

## Related docs

- [event-model.md](event-model.md)
- [api-contract.md](api-contract.md)
- [filter-contract.md](filter-contract.md)
- [middleware-contract.md](middleware-contract.md)
- [di-model.md](di-model.md)
- [message-api-contract.md](message-api-contract.md)
