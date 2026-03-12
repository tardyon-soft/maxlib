# API Contract: Core Public Interfaces

## Status

Этот документ фиксирует контракт core public interfaces framework.
Это спецификация API и границ ответственности, а не реализация.

## Цели документа

- Зафиксировать стабильные public surface точки framework.
- Разделить ответственность между runtime, client и DSL слоями.
- Упростить принятие архитектурных решений без утечки внутренних деталей.

## Общие правила контракта

- Интерфейсы описывают поведение, а не внутреннюю реализацию.
- Любые MAX-специфичные детали должны проходить через `MaxBotClient` и model/request объекты.
- `Dispatcher`/`Router` отвечают за orchestration, а не за HTTP/serialization.
- `Filter` и `Middleware` должны быть композиционными и предсказуемыми по порядку.
- `Context`/`FSMContext` являются основными точками handler ergonomics.

## Update ingestion layer (Sprint 2)

### Назначение

Контракты ingestion-слоя задают единый transport boundary для получения `Update`
из polling и webhook и передачи их в общий внутренний flow.

### Core сущности

- `UpdateSource` — lifecycle-контракт источника update (`start`/`stop`).
- `PollingUpdateSource` — источник update через long polling.
- `WebhookUpdateSource` — источник update через webhook ingress.
- `UpdateSink` (`UpdateConsumer`) — единая точка приёма normalized `Update`.
- `LongPollingRunner` — управляющий цикл long polling ingestion.
- `WebhookReceiver` — boundary между HTTP webhook endpoint и ingestion source.
- `UpdatePipeline` — unified ingress контракт для downstream обработки.

### Границы Sprint 2

- В scope: transport ingestion (polling + webhook), normalization и доставка в sink.
- Вне scope: полноценный dispatcher/router runtime, middleware runtime, FSM runtime.

Детальная спецификация ingestion contracts:
- [update-ingestion.md](update-ingestion.md)

## MaxBotClient

### Назначение

Низкоуровневый клиент для вызовов MAX API: отправка/редактирование сообщений, callback-ответы, upload flow, webhook/polling методы.

### Границы ответственности

Отвечает за:
- выполнение MAX API requests;
- маппинг transport/domain ошибок в типизированные исключения;
- platform constraints validation, где это возможно до отправки.

Не отвечает за:
- маршрутизацию update на handlers;
- middleware/filter/fsm orchestration;
- бизнес-правила приложения.

### Пример использования

```java
MaxBotClient client = MaxBotClient.builder()
    .token(token)
    .baseUrl("https://api.max.ru")
    .build();

client.sendMessage(
    SendMessageRequest.builder()
        .chatId(chatId)
        .text("Hello")
        .build()
);
```

## Dispatcher

### Назначение

Корневой orchestrator единого update pipeline для polling и webhook входа.

### Границы ответственности

Отвечает за:
- регистрацию router graph;
- запуск цепочки middleware;
- error boundary и lifecycle hooks.

Не отвечает за:
- конкретный transport (детали long polling/webhook сервера);
- хранение бизнес-состояния вне FSM абстракций;
- low-level HTTP к MAX.

### Пример использования

```java
Dispatcher dispatcher = Dispatchers.standard(client)
    .outerMiddleware(new LoggingMiddleware())
    .includeRouter(mainRouter)
    .onError((ctx, ex) -> ctx.reply("Unexpected error"));
```

## Router

### Назначение

Модульная маршрутизация update-событий по typed handlers и filters.

### Границы ответственности

Отвечает за:
- регистрацию handler-ов по update типам;
- локальные middleware;
- иерархию router-ов (include child routers).

Не отвечает за:
- запуск runtime цикла;
- создание HTTP requests напрямую;
- global error policy (за пределами router scope).

### Пример использования

```java
Router router = new Router("main");

router.message(F.message().text().startsWith("/start"), ctx ->
    ctx.reply("Привет")
);

router.callback(F.callback().data().startsWith("order:"), ctx ->
    ctx.answerCallback("OK")
);
```

Детальная модель `includeRouter`, observer-ов и resolution порядка зафиксирована в ADR:
- [adr/0001-router-model.md](adr/0001-router-model.md)
Детальный callback contract (event model, handler API, answer abstraction, lifecycle):
- [callback-contract.md](callback-contract.md)

## Filter<TContext>

### Назначение

Декларативный predicate-контракт для отбора событий до вызова handler.

### Границы ответственности

Отвечает за:
- проверку условия матчинга;
- композицию условий (`and`, `or`, `not`).

Не отвечает за:
- сайд-эффекты (HTTP/DB вызовы);
- изменение runtime context;
- fallback routing policy.

### Пример использования

```java
Filter<MessageContext> start = F.message().text().equalsTo("/start");
Filter<MessageContext> privateChat = F.message().chat().type().isPrivate();

router.message(start.and(privateChat), ctx -> ctx.reply("Private start"));
```

Детальный контракт filter API, built-in набора и композиции:
- [filter-contract.md](filter-contract.md)

## Middleware

### Назначение

Cross-cutting цепочка обработки: logging, metrics, auth, tracing, retries.

### Границы ответственности

Отвечает за:
- pre/post обработку вокруг handler execution;
- передачу управления дальше (`next.invoke()`).

Не отвечает за:
- регистрацию маршрутов;
- прямой выбор handler (это делает routing+filters);
- хранение domain state как primary storage.

### Пример использования

```java
dispatcher.outerMiddleware((ctx, next) -> {
    long startedAt = System.nanoTime();
    try {
        return next.invoke();
    } finally {
        metrics.recordLatency(ctx.updateType(), System.nanoTime() - startedAt);
    }
});
```

Детальный контракт outer/inner middleware, enrichment и error propagation:
- [middleware-contract.md](middleware-contract.md)

## Context

### Назначение

Единая typed оболочка вокруг текущего update и framework services для handler-level DX.

### Границы ответственности

Отвечает за:
- доступ к update payload (`message`, `callback`, `chat`, `user`);
- helper-операции (`reply`, `answerCallback`, `bot()`);
- резолв зависимостей из runtime container.

Не отвечает за:
- хранение долгоживущего состояния (кроме ссылок на FSMContext);
- конфигурацию dispatcher/router;
- transport lifecycle.

### Пример использования

```java
router.message(F.message().text().startsWith("/help"), (Context ctx) -> {
    ctx.reply("Доступные команды: /start, /help");
});
```

Детальная модель dependency injection для handler аргументов:
- [di-model.md](di-model.md)

## FSMContext

### Назначение

Контракт для работы с диалоговым состоянием: state machine + data bag.

### Границы ответственности

Отвечает за:
- чтение/установку/очистку текущего state;
- хранение state-associated key/value data;
- взаимодействие с настроенным FSM storage.

Не отвечает за:
- маршрутизацию update;
- описание сценария диалога целиком (это уровень Scene);
- transport/client вызовы.

### Пример использования

```java
router.message(Command.start(), (Context ctx, FSMContext fsm) -> {
    fsm.setState("checkout.waiting_email");
    ctx.reply("Введите email");
});
```

Детальный FSM/scenes contract (`FSMContext`, storage abstraction, scopes, state filters, scenes, wizard):
- [fsm-scenes-contract.md](fsm-scenes-contract.md)

## Scene

### Назначение

Высокоуровневая модель шага/ветки диалога поверх FSMContext (wizard-style flows).

### Границы ответственности

Отвечает за:
- описание шагов сцены и переходов между ними;
- hooks жизненного цикла (`onEnter`, `onExit`);
- ограничение области handler-ов внутри сцены.

Не отвечает за:
- low-level persistence (делегируется FSM storage);
- глобальную маршрутизацию вне сцены;
- client transport.

### Пример использования

```java
Scene checkout = Scene.builder("checkout")
    .step("email", (ctx, fsm) -> ctx.reply("Введите email"))
    .step("confirm", (ctx, fsm) -> ctx.reply("Подтвердите заказ"))
    .build();

router.scene(checkout);
```

Детали `Scene`/`SceneManager` lifecycle и wizard-style API:
- [fsm-scenes-contract.md](fsm-scenes-contract.md)

## MessageBuilder

### Назначение

Fluent builder для типобезопасной сборки исходящих сообщений и связанных опций.

### Границы ответственности

Отвечает за:
- сборку request модели сообщения;
- локальную валидацию данных перед отправкой;
- интеграцию с keyboard/attachment builders.

Не отвечает за:
- отправку HTTP запроса напрямую;
- роутинг handler execution;
- хранение диалогового состояния.

### Пример использования

```java
MessageBuilder msg = Messages.text("Выберите действие")
    .format(Format.MARKDOWN)
    .keyboard(k -> k.row(
        Buttons.callback("Оплатить", "pay:1"),
        Buttons.link("Сайт", "https://example.com")
    ));

ctx.reply(msg);
```

Детальный контракт Message API (`send/edit/delete/reply`, formatting, notify, attachments, keyboard):
- [message-api-contract.md](message-api-contract.md)
Детальный upload/media contract (input abstractions и multi-step upload flow):
- [upload-media-contract.md](upload-media-contract.md)

## KeyboardBuilder

### Назначение

Fluent DSL для сборки keyboard layout (inline/reply) с typed button-ами.

### Границы ответственности

Отвечает за:
- row/column структуру клавиатуры;
- typed button variants (`callback`, `link`, supported platform actions);
- валидацию ограничений layout/button data.

Не отвечает за:
- отправку сообщения;
- обработку callback событий (это router/handler уровень);
- persistence.

### Пример использования

```java
KeyboardBuilder keyboard = Keyboards.inline(k -> k
    .row(
        Buttons.callback("1", "vote:1"),
        Buttons.callback("2", "vote:2")
    )
    .row(Buttons.link("Правила", "https://example.com/rules"))
);

ctx.reply(Messages.text("Голосование").keyboard(keyboard));
```

## Контракт совместимости

- Эти интерфейсы считаются основой public API и эволюционируют обратно-совместимо.
- Ломающие изменения допускаются только с явной миграционной документацией.
- Если MAX API ограничивает поведение, приоритет у честного platform mapping, а не у Telegram parity.
