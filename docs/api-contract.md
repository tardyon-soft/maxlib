# API Contract: Core Public Interfaces

## Status

Документ отражает текущее состояние public contracts framework на этапе Sprint 9.
Это контракт поведения и границ ответственности, не описание внутренней реализации.

## Contract Rules

- Runtime не дублирует HTTP/serialization слой: всё MAX API interaction идёт через `MaxBotClient`.
- Polling и webhook обязаны сходиться в единый update pipeline.
- `Dispatcher`/`Router` отвечают за orchestration dispatch, а не за transport.
- Filters/middleware/DI/FSM остаются composable и predictable.
- Если поведение MAX ограничивает API, приоритет у честной платформенной адаптации.

## Ingestion Contracts

Ключевые сущности:

- `UpdateConsumer` (`handle(Update) -> CompletionStage<UpdateHandlingResult>`) — основной ingestion target.
- `UpdateSink` — backward-compatible alias.
- `PollingUpdateSource` (`poll(PollingFetchRequest) -> PollingBatch`).
- `LongPollingRunner` (`start`, `stop`, `shutdown`, `isRunning`).
- `WebhookSecretValidator` (`validate(secretHeader)`).
- `WebhookReceiver` (`receive(WebhookRequest) -> WebhookReceiveResult`).
- `UpdatePipeline` — unified ingestion flow contract для polling/webhook.

Граница ответственности:

- ingestion отвечает за доставку normalized `Update` в runtime;
- routing/middleware/handler logic начинается после передачи в dispatcher.

Детали: [update-ingestion.md](update-ingestion.md), [event-model.md](event-model.md).

## Runtime Contracts

### `Dispatcher`

Назначение:

- root orchestrator runtime pipeline;
- владелец root router graph;
- единая dispatch entrypoint: `feedUpdate(Update)`.

Ключевые методы:

- `includeRouter(...)`, `includeRouters(...)`
- `outerMiddleware(...)`
- `feedUpdate(Update)`
- `handle(Update)` (ingestion integration)
- `registerService(...)`, `registerApplicationData(...)`
- `withBotClient(...)`, `withUploadService(...)`, `withFsmStorage(...)`, `withStateScope(...)`,
  `withSceneRegistry(...)`, `withSceneStorage(...)`

### `Router`

Назначение:

- модульная регистрация handlers и router composition.

Ключевые методы:

- `message(...)`, `callback(...)`, `update(...)`, `error(...)`
- overloads с `Filter<TEvent>`
- reflective registration (`target + method`)
- `innerMiddleware(...)`
- `includeRouter(...)`

Routing semantics:

- deterministic DFS traversal по router tree;
- first-match execution;
- `HANDLED` останавливает дальнейший поиск;
- ошибки переходят в `error` observer boundary.

Детали: [runtime-contract.md](runtime-contract.md), [adr/0001-router-model.md](adr/0001-router-model.md).

## Filter and Middleware Contracts

### `Filter<TEvent>`

- `test(event)` и optional `test(event, RuntimeContext)`
- возвращает `FilterResult`: `MATCHED`, `NOT_MATCHED`, `FAILED`
- поддерживает композицию `and/or/not`
- enrichment data может быть прокинут в runtime context

### `Middleware`

- единый контракт `invoke(RuntimeContext, MiddlewareNext)`
- outer middleware: вокруг полного update dispatch
- inner middleware: вокруг matched handler execution
- поддерживается short-circuit

Pipeline order:

1. outer middleware
2. filters
3. inner middleware
4. handler invocation

Детали: [filters-and-middleware.md](filters-and-middleware.md), [middleware-contract.md](middleware-contract.md), [filter-contract.md](filter-contract.md).

## DI / Invocation Contracts

Ключевые сущности:

- `HandlerInvoker`
- `HandlerParameterResolver`
- `ResolverRegistry`
- `ContextualEventHandler<TEvent>`
- reflective handler path через `ReflectiveEventHandler`

Resolution sources:

- core runtime/update objects (`RuntimeContext`, `Update`, `Message`, `Callback`, `User`, `Chat`)
- filter/middleware enrichment data
- shared application services/data
- FSM/scenes runtime objects (`FSMContext`, `SceneManager`, `WizardManager`) при конфигурации

Детали: [di-and-invocation.md](di-and-invocation.md), [di-model.md](di-model.md).

## Messaging Contracts

Ключевые сущности:

- `MessageTarget`
- `MessageBuilder` / `Messages`
- `KeyboardBuilder` / `Buttons` / `Keyboards`
- `MessagingFacade`
- `CallbackFacade` / `CallbackContext`
- `ChatActionsFacade`

Назначение:

- удобный runtime-facing API для send/edit/delete/reply/callback answer/chat actions;
- маппинг в existing typed SDK without transport duplication.

Детали: [messaging-api.md](messaging-api.md), [message-api-contract.md](message-api-contract.md), [callback-contract.md](callback-contract.md).

## Upload / Media Contracts

Ключевые сущности:

- `InputFile`
- `UploadService`
- transfer gateways (`multipart` / `resumable`)
- `UploadResult`
- media abstractions (`ImageAttachment`, `FileAttachment`, `VideoAttachment`, `AudioAttachment`)
- `MediaMessagingFacade`

Назначение:

- скрыть multi-step upload flow MAX;
- дать high-level send/reply media ergonomics поверх существующего messaging/runtime слоя.

Детали: [upload-and-media.md](upload-and-media.md), [upload-media-contract.md](upload-media-contract.md).

## FSM / Scenes Contracts

Ключевые сущности:

- `FSMStorage` (+ `MemoryStorage` baseline)
- `FSMContext`
- `StateScope`, `StateKeyStrategy`, `StateFilter`
- `SceneRegistry`, `SceneManager`, `SceneStorage`
- `WizardManager`

Назначение:

- stateful dialog flows с предсказуемой storage boundary;
- интеграция в routing и DI без отдельного runtime.

Детали: [fsm-and-scenes.md](fsm-and-scenes.md), [fsm-scenes-contract.md](fsm-scenes-contract.md).

## Spring Starter and Testkit Contracts

- Spring starter: [spring-starter.md](spring-starter.md)
- Testkit: [testkit.md](testkit.md)

## Compatibility Policy

- Public API эволюционирует обратно-совместимо.
- Ломающие изменения требуют явной миграционной документации.
- Документация должна быть синхронизирована с фактическим кодом в том же change set.
