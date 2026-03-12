# Product Spec: Public API and Developer Experience

## Status

Документ фиксирует целевой публичный API и желаемый developer experience.
Это спецификация, а не реализация.

## Scope

- Описывает, как пользователь framework пишет бота.
- Фокус на ergonomics в стиле aiogram 3, адаптированный под Java и MAX API.
- Не задаёт внутренние детали реализации runtime/client.

## DX goals

- Минимум boilerplate для типичных bot flows.
- Единая модель обработки Update независимо от polling/webhook.
- Читаемый DSL для routing, filters и middleware.
- Явная типобезопасность и предсказуемая обработка ошибок.
- Удобная инъекция зависимостей в handler.

## Core API surfaces

### Dispatcher

Назначение:
- корневой orchestrator update pipeline;
- подключение router'ов;
- запуск runtime (polling/webhook через отдельные adapters).

Желаемый API (пример):

```java
Dispatcher dispatcher = Dispatchers.standard(botClient)
    .outerMiddleware(new LoggingMiddleware())
    .outerMiddleware(new ErrorHandlingMiddleware())
    .includeRouter(mainRouter)
    .includeRouter(adminRouter);
```

Ожидаемые возможности:
- `includeRouter(Router router)`;
- `outerMiddleware(Middleware middleware)`;
- `innerMiddleware(Middleware middleware)`;
- `onError(ErrorHandler handler)`.

### Router

Назначение:
- модульная маршрутизация по типам update/event.

Желаемый API (пример):

```java
Router mainRouter = new Router("main");

mainRouter.message(Command.start(), ctx ->
    ctx.reply(Messages.text("Привет!"))
);

mainRouter.callback(CallbackData.prefix("order:"), ctx ->
    ctx.answerCallback("OK")
);

dispatcher.includeRouter(mainRouter);
```

Ожидаемые возможности:
- `message(Filter<MessageContext> filter, MessageHandler handler)`;
- `callback(Filter<CallbackContext> filter, CallbackHandler handler)`;
- `includeRouter(Router child)`.

### Handlers

Назначение:
- обработка бизнес-логики в strongly-typed контексте.

Желаемый API (пример):

```java
router.message(
    F.message().text().startsWith("/start"),
    (MessageContext ctx) -> ctx.reply("Добро пожаловать")
);

router.callback(
    F.callback().data().startsWith("pay:"),
    (CallbackContext ctx) -> ctx.answerCallback("Оплата принята")
);
```

Дополнительно:
- sync и async формы handler (`void` / `CompletionStage<?>`);
- единый error boundary через dispatcher error handler.

### Filters

Назначение:
- декларативная маршрутизация без ручного `if/else`.

Желаемый API (пример):

```java
Filter<MessageContext> startCommand = F.message().text().equalsTo("/start");
Filter<MessageContext> privateChat = F.message().chat().type().isPrivate();

router.message(startCommand.and(privateChat), ctx -> {
    ctx.reply("Private start command");
});
```

Ожидаемые возможности:
- фабрика `F` для разных update типов;
- композиция `and`, `or`, `not`;
- предикаты по text, callback data, chat/user attributes.

### Middleware

Назначение:
- cross-cutting логика: logging, auth, metrics, retries, tracing.

Желаемый API (пример):

```java
dispatcher.outerMiddleware((ctx, next) -> {
    long started = System.nanoTime();
    try {
        return next.invoke();
    } finally {
        metrics.recordLatency(ctx.updateType(), System.nanoTime() - started);
    }
});

router.innerMiddleware(new AuthorizationMiddleware());
```

Ожидаемые возможности:
- outer middleware на уровне dispatcher;
- inner middleware на уровне router/observer;
- deterministic execution order.

### Context injection

Назначение:
- убрать ручное извлечение зависимостей из service locator.

Желаемый API (пример):

```java
router.message(
    F.message().text().startsWith("/profile"),
    (Message msg, Bot bot, UserSession session, UserService users) -> {
        UserProfile profile = users.load(session.userId());
        bot.send(Messages.text("User: " + profile.name()).chatId(msg.chatId()));
    }
);
```

Ожидаемые источники инъекции:
- update objects (`Message`, `Callback`, `Chat`, `User`);
- framework objects (`Bot`, `FsmContext`);
- app services из DI контейнера (включая Spring).

### Message builder

Назначение:
- удобная сборка исходящих сообщений с валидацией ограничений платформы.

Желаемый API (пример):

```java
ctx.reply(
    Messages.text("Выберите действие")
        .format(Format.MARKDOWN)
        .keyboard(k -> k.row(
            Buttons.callback("Оплатить", "pay:1"),
            Buttons.link("Сайт", "https://example.com")
        ))
);
```

Ожидаемые возможности:
- builders для text/media/callback responses;
- platform-aware validation до отправки;
- единые объекты request для client-core.

### Keyboard builder

Назначение:
- fluent API для inline/reply клавиатур.

Желаемый API (пример):

```java
InlineKeyboard keyboard = Keyboards.inline(k -> k
    .row(
        Buttons.callback("1", "vote:1"),
        Buttons.callback("2", "vote:2")
    )
    .row(Buttons.link("Правила", "https://example.com/rules"))
);

ctx.reply(Messages.text("Голосование").keyboard(keyboard));
```

Ожидаемые возможности:
- row-based DSL;
- typed buttons (`callback`, `link`, platform-supported variants);
- compile-time friendly API без map/json handcrafting.

### FSM

Назначение:
- управление диалоговыми состояниями, сценами и wizard flows.

Желаемый API (пример):

```java
router.message(Command.start(), (MessageContext ctx, FsmContext fsm) -> {
    fsm.setState(CheckoutStates.WAITING_EMAIL);
    ctx.reply("Введите email");
});

router.message(
    F.message().stateIs(CheckoutStates.WAITING_EMAIL),
    (MessageContext ctx, FsmContext fsm) -> {
        fsm.data().put("email", ctx.message().text());
        fsm.setState(CheckoutStates.WAITING_CONFIRM);
        ctx.reply("Подтвердите заказ");
    }
);
```

Ожидаемые возможности:
- storage abstraction (`memory`, `redis`, custom);
- scoped state key strategy (chat/user/thread);
- scenes/wizard поверх базового FSM API.

## Consistency rules for public API

- Имена API должны быть стабильными и предсказуемыми.
- Публичный API не должен раскрывать transport-level детали MAX HTTP.
- По возможности immutable request/response DTO.
- Ошибки framework и client должны быть типизированы.
- Любая MAX-специфичная неоднозначность должна отражаться в docs/notes.

## Out of scope in this document

- Детали internal package structure и конкретных runtime классов.
- Конкретные реализации storage/transport.
- Полный список MAX методов.

## Traceability

- MAX API docs: [https://dev.max.ru/docs-api](https://dev.max.ru/docs-api)
- aiogram reference for DX patterns: [https://docs.aiogram.dev/](https://docs.aiogram.dev/)
