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

- `UpdateSource` — базовый lifecycle boundary transport-source.
- `PollingUpdateSource` — pull-based источник update через long polling (`poll` -> `PollingBatch`).
- `WebhookUpdateSource` — источник update через webhook ingress.
- `UpdateConsumer` — предпочтительная async-точка приёма normalized `Update`.
- `UpdateSink` — deprecated backward-compatible alias для `UpdateConsumer`.
- `UpdateConsumer`/`UpdateSink` возвращают `UpdateHandlingResult` (`SUCCESS`/`FAILURE`).
- `LongPollingRunner` — lifecycle-контракт long polling loop (`start`/`stop`/`shutdown`/`isRunning`).
- `LongPollingRunnerConfig` — lifecycle/ownership настройки (`shutdownTimeout`, resource ownership).
- `PollingMarkerState` — marker progression boundary (in-memory now, persistent later).
- `WebhookRequest` — framework-agnostic raw webhook request (`body` + `headers`).
- `WebhookSecretValidator` — контракт проверки `X-Max-Bot-Api-Secret`.
- `WebhookSecretValidationResult`/`WebhookValidationError` — результат и причина отказа валидации.
- `WebhookReceiver` — boundary между HTTP webhook endpoint и ingestion source
  (`WebhookRequest` -> `WebhookReceiveResult`).
- `WebhookReceiveResult` — result contract для web adapter integration (включая `OVERLOADED`).
- `WebhookReceiverConfig` — базовый overload control (`maxInFlightRequests`).
- `UpdatePipeline` — unified ingress контракт для downstream обработки.
- `UpdatePipelineContext`/`UpdatePipelineResult` — transport context и результат pipeline delivery.
- `UpdatePipelineHook` — internal extension point для logging/metrics/dispatcher bridge.

### Границы Sprint 2

- В scope: transport ingestion (polling + webhook), normalization и доставка в sink.
- Вне scope: полноценный dispatcher/router runtime, middleware runtime, FSM runtime.

Детальная спецификация ingestion contracts:
- [update-ingestion.md](update-ingestion.md)

## Runtime layer (Sprint 3)

### Назначение

Контракты runtime-слоя фиксируют минимальный dispatch pipeline поверх ingestion layer:
`Dispatcher` + `Router` + observer/handler execution semantics.

### Границы Sprint 3

- В scope: dispatcher/router foundation, observer registration, first-match dispatch.
- Вне scope: полноценный filter DSL, middleware runtime, FSM/scenes runtime.

Детальная спецификация runtime contracts:
- [runtime-contract.md](runtime-contract.md)

## Filters and middleware layer (Sprint 4)

### Назначение

Контракты Sprint 4 задают базовый runtime pipeline поверх Sprint 3:
outer middleware -> filters -> inner middleware -> handler.

### Границы Sprint 4

- В scope: базовый filter DSL, middleware execution order, request-scoped context enrichment.
- Вне scope: полноценный DI parameter resolution, FSM/scenes runtime.

Детальная спецификация Sprint 4 contracts:
- [filters-and-middleware.md](filters-and-middleware.md)

## DI and invocation layer (Sprint 5)

### Назначение

Контракты Sprint 5 задают runtime invocation boundary:
- вызов handler-а через `HandlerInvoker`;
- резолв параметров через `HandlerParameterResolver` + `ResolverRegistry`.
- Java-friendly signature model: `ContextualEventHandler<TEvent>` (`event + RuntimeContext`).
- typed runtime data model: `RuntimeDataContainer` + `RuntimeDataKey<T>` + source scopes.
- method-based adapter path: `ReflectiveEventHandler<TEvent>` + `DefaultHandlerInvoker`.
- built-in resolver coverage: `Update`, `Message`, `Callback`, `User`, `Chat`, `RuntimeContext`.
- enrichment-derived resolver coverage: filter data + middleware data (by unique type).

### Границы Sprint 5

- В scope: parameter resolution contract и invocation diagnostics.
- Вне scope: полноценный IoC container, Spring integration, FSM/scenes runtime.

Детальная спецификация Sprint 5 contracts:
- [di-and-invocation.md](di-and-invocation.md)

## Messaging layer (Sprint 6)

### Назначение

Контракты Sprint 6 задают high-level messaging ergonomics поверх существующих runtime и SDK слоёв:
- send/reply/edit/delete builders;
- keyboard/buttons builders;
- callback answer и chat actions abstractions.

### Границы Sprint 6

- В scope: high-level API и builder contracts.
- Вне scope: upload/media subsystem, FSM/scenes, Spring starter integration.

Детальная спецификация Sprint 6 contracts:
- [messaging-api.md](messaging-api.md)

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
- [fsm-and-scenes.md](fsm-and-scenes.md)

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
- [fsm-and-scenes.md](fsm-and-scenes.md)

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
Актуальный Sprint 7 upload/media contract (InputFile, UploadService, multipart/resumable, media facade):
- [upload-and-media.md](upload-and-media.md)

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
