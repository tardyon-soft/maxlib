# Dependency Injection Model Specification

## Status

Документ фиксирует DI model framework для handler argument resolution.
Это спецификация контракта публичного поведения, а не реализация.

## Goals

- Предсказуемо резолвить аргументы handler-ов без ручного service locator кода.
- Объединить источники данных framework в единый resolution pipeline.
- Сделать injection типобезопасным и диагностируемым.

## Terms

- `Handler`: функция/метод, зарегистрированная в observer (`message`, `callback`).
- `Invocation Context`: request-scoped объект текущего update processing.
- `Resolver`: компонент, который пытается предоставить значение для параметра handler-а.

## Injection sources (MVP)

В резолве параметров участвуют следующие источники:

1. Update/context objects
- `Context`, `MessageContext`, `CallbackContext`;
- payload objects (`Message`, `Callback`, `Chat`, `User`) если доступны для текущего `UpdateType`.

2. Framework services
- `Bot` (высокоуровневый API отправки команд);
- `MaxBotClient` (низкоуровневый API client-core);
- `FSMContext` (если FSM включён и scope определён);
- runtime utility services (clock, serializer, validator) как framework-managed services.

3. Middleware-produced data
- request-scoped attributes, добавленные middleware.
- доступ по typed ключу (например, `ContextKey<T>`) или через annotation binding.

4. Filter-produced data
- bindings, созданные matched filter-ами (`FilterBindings`).
- типизированные значения, извлечённые из text/callback/state фильтров.

5. Application DI container
- сервисы приложения (Spring beans или runtime registry services).

## Resolution order contract

Для каждого параметра handler-а резолв выполняется строго по порядку:

1. Explicit context/update objects.
2. Framework services.
3. Filter-produced data.
4. Middleware-produced attributes.
5. Application DI container.

Принципы:
- first-success-wins;
- резолв не должен быть nondeterministic;
- одинаковый параметр в одной сигнатуре резолвится одинаковым источником.

## Parameter matching rules

### By type (default)

Если тип параметра уникально резолвится из текущего шага, значение инжектится по типу.

Пример:

```java
(MessageContext ctx, Bot bot, UserService users) -> { ... }
```

### By qualifier/annotation (for ambiguous or extracted data)

Используются marker-аннотации:
- `@FromFilter(FilterKey<T>)` для filter-produced data;
- `@FromContext(ContextKey<T>)` для middleware/request attributes;
- `@Named("...")` или container-specific qualifier для application DI.

Пример:

```java
(CallbackContext ctx,
 @FromFilter(ORDER_ID) Long orderId,
 @FromContext(CORRELATION_ID) String correlationId) -> { ... }
```

### Optional values

Разрешены параметры в форме:
- `Optional<T>`;
- nullable (если поддержано выбранной аннотацией/контрактом);
- значения с дефолтом через explicit wrapper (post-MVP extension).

Если обязательный параметр не резолвится:
- handler не вызывается;
- вызывается dispatcher error policy с диагностикой resolution failure.

## Framework services contract

- Framework services request-safe и доступны без явной регистрации пользователем.
- Версии сервисов и их API считаются частью framework contract.
- Если конкретный сервис недоступен в текущем runtime профиле, это fail-fast ошибка конфигурации на startup.

## Middleware data contract

Middleware enrichment передаётся через `Context.attributes()` и может участвовать в injection.

Пример enrichment:

```java
ContextKey<String> CORRELATION_ID = ContextKey.of("correlationId", String.class);

dispatcher.outerMiddleware((ctx, next) -> {
    ctx.attributes().put(CORRELATION_ID, UUID.randomUUID().toString());
    return next.invoke();
});
```

Пример резолва в handler:

```java
(MessageContext ctx, @FromContext(CORRELATION_ID) String correlationId) -> {
    ctx.reply("trace=" + correlationId);
}
```

## Filter-produced data contract

Если matched filter экспортирует bindings, они доступны в injection и в `Context`.

Пример filter extracting contract:

```java
FilterKey<Long> ORDER_ID = FilterKey.of("orderId", Long.class);

router.callback(
    F.callback().data().pattern("order:(\\d+)", ORDER_ID, Long::parseLong),
    (CallbackContext ctx, @FromFilter(ORDER_ID) Long orderId, Bot bot) -> {
        bot.send(Messages.text("Order " + orderId).chatId(ctx.chatId()));
    }
);
```

Collision rules:
- filter bindings приоритетнее middleware attributes для `@FromFilter`;
- `@FromContext` читает только middleware/context attributes;
- неявный резолв по типу не должен смешивать filter/context источники при наличии ambiguity.

## Handler signature examples

### Minimal

```java
(Context ctx) -> ctx.reply("pong")
```

### Typed message + service

```java
(Message message, Bot bot, UserService userService) -> {
    UserProfile p = userService.load(message.userId());
    bot.send(Messages.text("Hi, " + p.name()).chatId(message.chatId()));
}
```

### Callback + filter data + middleware data

```java
(CallbackContext ctx,
 @FromFilter(ORDER_ID) Long orderId,
 @FromContext(CORRELATION_ID) String correlationId,
 OrderService orders) -> {
    orders.markSeen(orderId, correlationId);
    ctx.answerCallback("OK");
}
```

### FSM-enabled

```java
(MessageContext ctx, FSMContext fsm, CheckoutService checkout) -> {
    checkout.captureEmail(ctx.message().text());
    fsm.setState("checkout.waiting_confirm");
    ctx.reply("Подтвердите заказ");
}
```

## Error handling and diagnostics

- Resolution errors должны содержать имя handler-а, индекс/имя параметра и источник, где поиск завершился.
- Ambiguous injection (несколько кандидатов в одном источнике без qualifier) — конфигурационная ошибка.
- Diagnostics должны быть доступны в testkit assertions.

## Non-goals

- Детали container implementation (Spring/native registry internals).
- Compile-time code generation для injection (возможное post-MVP улучшение).
- Полный список всех framework utility services.

## Related docs

- [api-contract.md](api-contract.md)
- [filter-contract.md](filter-contract.md)
- [middleware-contract.md](middleware-contract.md)
- [event-model.md](event-model.md)
