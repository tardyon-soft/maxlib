# DI and Handler Invocation Contract (Sprint 5)

## Status

Документ фиксирует контракт Sprint 5 для runtime invocation и parameter resolution.
Это спецификация API/поведения, а не полная реализация.

## Sprint 5 goal

Сделать handler signatures ergonomic:
- developer описывает параметры handler-а;
- framework резолвит их из runtime context и известных источников;
- бизнес-логика не зависит от ручного извлечения данных из сырых объектов.

## Sprint 5 boundaries

В scope Sprint 5:
- runtime contract для `HandlerInvoker`, `HandlerParameterResolver`, `ResolverRegistry`;
- parameter resolution model и deterministic resolution order;
- источники данных: runtime/event/filter/middleware/shared services.

Вне scope Sprint 5:
- полноценный IoC/DI container;
- Spring integration и bean lifecycle;
- FSM/scenes/state storage runtime;
- service locator как основной public API.

## Core contracts

## Chosen invocation model (Sprint 5.1)

Выбранный подход: typed functional interfaces как primary API + runtime extension points.

- Базовый контракт остаётся `EventHandler<TEvent>` (обратная совместимость с Sprint 3/4).
- Для Sprint 5 введён `ContextualEventHandler<TEvent>`:
  - сигнатура handler-а: `(TEvent event, RuntimeContext context) -> CompletionStage<Void>`;
  - удобно для Java-лямбд и без reflection-heavy magic.
- Method-based reflective handlers остаются возможным future extension,
  но не являются MVP-моделью Sprint 5.1.

Почему это практично для Java:
- типы параметров видны на этапе компиляции;
- API читается как обычные лямбды/functional interfaces;
- не нужен annotation-heavy runtime на раннем этапе;
- легко расширить до parameter resolvers без ломающего redesign.

### `HandlerInvoker`

Назначение:
- единая точка вызова handler-а после успешного route matching;
- orchestration parameter resolution + actual invocation.

Ответственность:
- получить handler descriptor (target + signature);
- запросить значения параметров у `ResolverRegistry`;
- вызвать handler sync/async и нормализовать результат.

Не отвечает за:
- выбор handler-а (это dispatcher/router/filter layer);
- transport ingestion детали;
- lifecycle shared services.

MVP implementation baseline:
- `DefaultHandlerInvoker` (reflection + lightweight metadata cache);
- supports method return types `void` and `CompletionStage<?>`.

### `HandlerParameterResolver`

Назначение:
- резолв одного параметра handler-а из одного конкретного источника.

Контракт (концептуально):
- принимает `ParameterDescriptor` + `InvocationContext`;
- возвращает `ResolvedParameter` или `UNRESOLVED`;
- не делает побочных эффектов.

Требования:
- deterministic behavior;
- типобезопасность;
- понятная диагностика при ошибке.

MVP built-ins:
- `RuntimeContextParameterResolver`;
- `UpdateParameterResolver`;
- `EventParameterResolver`;
- `ApplicationDataParameterResolver`.

### `ResolverRegistry`

Назначение:
- упорядоченный реестр resolvers и policy выбора.

Ответственность:
- хранить resolver chain в фиксированном порядке;
- применять first-success-wins policy;
- возвращать diagnostics при unresolved/ambiguous cases.

MVP contract:
- `ResolverRegistry.register(...)` добавляет resolver в конец chain;
- resolution order определяется порядком регистрации.

## Invocation context model

`InvocationContext` (runtime-internal concept) агрегирует:
- текущий `Update` и resolved event object (`Message`, `Callback`, ...);
- `RuntimeContext` (typed attributes + enrichment map);
- matched filter enrichment;
- middleware enrichment;
- shared application services registry.

`InvocationContext` request-scoped:
- живёт в рамках одного dispatch;
- не шарится между update processing.

### Runtime data container baseline

Базовый контейнер данных Sprint 5:
- `RuntimeDataContainer` + typed keys `RuntimeDataKey<T>`;
- source scope model: `FRAMEWORK`, `FILTER`, `MIDDLEWARE`, `APPLICATION`;
- explicit conflict policy для одинакового key-name с разными значениями;
- explicit override через `replace(...)` (no implicit shadowing).

Этот контейнер является базой для будущих `HandlerParameterResolver` implementations.

## Parameter categories (MVP for Sprint 5)

Framework должен уметь резолвить:

1. Core runtime objects
- `RuntimeContext`;
- `DispatchResult`-related metadata (если доступно на текущей фазе);
- framework runtime helper objects (ограниченный набор).

2. Event/update objects
- `Update`;
- current event payload (`Message`, `Callback`);
- производные объекты (`Chat`, `User`) при наличии в event.

3. Filter-produced data
- enrichment из `FilterResult.MATCHED`;
- доступ по explicit key/qualifier (не magic string lookup по умолчанию).

4. Middleware-produced data
- enrichment/attributes, добавленные outer/inner middleware;
- доступ по typed `ContextKey<T>` или explicit qualifier.

5. Shared services/application data
- заранее зарегистрированные сервисы приложения;
- lookup по типу и/или qualifier.

## Resolution order contract

Базовый order (deterministic):
1. Runtime core objects.
2. Event/update objects.
3. Filter-produced data.
4. Middleware/runtime-context data.
5. Shared application services.

Правила:
- first-success-wins;
- unresolved обязательный параметр -> invocation failure;
- ambiguous match без qualifier -> resolution error.

## Error and diagnostics contract

Resolution failure должен включать:
- handler id/signature;
- parameter name/index/type;
- этап (какой resolver chain segment завершился неуспехом);
- reason (`UNRESOLVED`, `AMBIGUOUS`, `TYPE_MISMATCH`).

Invocation boundary:
- parameter resolution errors считаются runtime dispatch errors;
- передаются в существующий error pipeline (`error` observer).

## Expected DX (handler signatures)

Примеры целевых сигнатур (концептуально):

```java
router.message((Message message) -> { ... });
```

```java
router.message((Message message, RuntimeContext ctx, MyService service) -> { ... });
```

```java
router.callback((Callback cb, User actor, OrderService orders) -> { ... });
```

```java
router.message(
    BuiltInFilters.textStartsWith("pay:"),
    (Message message, RuntimeContext ctx) -> {
        String suffix = ctx.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY, String.class).orElseThrow();
        // ...
    }
);
```

## Design constraints

- Не строить отдельный контейнер с bean lifecycle.
- Не делать service locator публичной основной моделью.
- Не смешивать resolution contracts с transport/web framework API.
- Сохранять совместимость с текущим dispatcher/filter/middleware pipeline.

## Related docs

- [api-contract.md](api-contract.md)
- [runtime-contract.md](runtime-contract.md)
- [filters-and-middleware.md](filters-and-middleware.md)
- [di-model.md](di-model.md)
