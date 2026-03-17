# API Contract: Current Public Surface

## Status

Документ описывает фактический публичный API в текущем коде (`max-*` модули), без планируемых/вымышленных слоёв.

## Core modules

- `max-model`: DTO/value objects (`Update`, `Message`, `Callback`, ids, request/response модели).
- `max-client-core`: typed MAX client (`MaxBotClient`) + transport/auth/serialization/errors.
- `max-dispatcher`: `Dispatcher`, `Router`, filters/middleware, runtime context, handler invocation.
- `max-fsm`: `FSMContext`, `FSMStorage`, scenes/wizard contracts.
- `max-spring-boot-starter`: Spring Boot autoconfiguration для polling/webhook.
- `max-testkit`: testing harness (`DispatcherTestKit`, fixtures, recorded API calls).

## Runtime contracts

### Dispatcher

Ключевые методы:

- `includeRouter(...)`, `includeRouters(...)`
- `outerMiddleware(...)`
- `feedUpdate(Update)`
- `handle(Update)` (ingestion boundary)
- `withBotClient(...)`, `withUploadService(...)`
- `withFsmStorage(...)`, `withStateScope(...)`, `withStateKeyStrategy(...)`
- `withSceneRegistry(...)`, `withSceneStorage(...)`, `withSceneStateBinding(...)`
- `registerService(...)`, `registerApplicationData(...)`

### Router

Ключевые методы:

- `message(...)`, `callback(...)`, `update(...)`, `error(...)`
- overloads с `Filter<TEvent>`
- reflective registration (`target + Method`)
- `innerMiddleware(...)`
- `includeRouter(...)`

Routing semantics:

- deterministic traversal по router tree;
- first matched handler wins внутри observer;
- `HANDLED` останавливает дальнейший поиск;
- runtime ошибки маршрутизируются в `error` observer.

## Filters and middleware

- `Filter<TEvent>`: `test(event)` + optional `test(event, RuntimeContext)`.
- `Middleware`: `invoke(RuntimeContext, MiddlewareNext)`.
- Pipeline order: outer middleware -> filters -> inner middleware -> handler.

Built-in filters (`BuiltInFilters`):

- `command`, `textEquals`, `textStartsWith`
- `callbackDataPresent`, `callbackDataEquals`, `callbackDataStartsWith`
- `chatType`, `fromUser`, `fromCallbackUser`, `hasAttachment`
- `state`, `stateIn`

## Handler invocation and DI

Reflective path: `ReflectiveEventHandler` + `DefaultHandlerInvoker` + `ResolverRegistry`.

Поддерживаются параметры из:

- core runtime/event objects (`RuntimeContext`, `Update`, `Message`, `Callback`, `User`, `Chat`)
- FSM/scenes (`FSMContext`, `SceneManager`, `WizardManager`) при runtime wiring
- filter/middleware enrichment (по уникальному типу)
- application data/services (`Dispatcher.registerService`/`registerApplicationData`)

Handler return types:

- `void`
- `CompletionStage<?>`

## Annotation sugar (additive)

Доступен дополнительный слой поверх `Router`:

- `@Route`, `@Message`, `@Text`, `@Command`, `@Callback`, `@CallbackPrefix`, `@State`
- `@UseFilters`, `@UseMiddleware`
- `AnnotatedRouteRegistrar` маппит аннотации в обычный `Router` registration.

Старый API не меняется и остаётся полностью рабочим.
