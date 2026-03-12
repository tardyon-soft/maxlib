# Naming Conventions and Package Strategy

## Status

Документ фиксирует naming conventions и package strategy проекта.
Это архитектурный и API-контракт для разработки, а не реализация.

## Goals

- Стабилизировать публичные имена API.
- Зафиксировать предсказуемую package-структуру по модулям.
- Разделить public API и внутреннюю инфраструктуру.

## Root package

Корневой package проекта:

`ru.max.botframework`

Правила:
- Все публичные API типы framework используют этот root package.
- Новые модули обязаны оставаться внутри `ru.max.botframework.*`.
- Смена root package считается ломающим изменением API.

## Module package strategy

Базовая стратегия по текущим модулям:

- `max-model` -> `ru.max.botframework.model`
- `max-client-core` -> `ru.max.botframework.client`
- `max-dispatcher` -> `ru.max.botframework.dispatcher`
- `max-fsm` -> `ru.max.botframework.fsm`
- `max-spring-boot-starter` -> `ru.max.botframework.spring`
- `max-testkit` -> `ru.max.botframework.testkit`

Рекомендация по расширению (post-MVP):
- `ru.max.botframework.filters`
- `ru.max.botframework.middleware`
- `ru.max.botframework.message`
- `ru.max.botframework.callback`
- `ru.max.botframework.upload`

## Public vs internal packages

### Public API packages

Публичные контракты размещаются в верхнем пространстве модуля, без маркеров internal.

Примеры:
- `ru.max.botframework.dispatcher.Dispatcher`
- `ru.max.botframework.dispatcher.Router`
- `ru.max.botframework.fsm.FSMContext` (контрактный target)

### Internal infrastructure packages

Внутренние реализации должны находиться только в пакетах с маркером:

- `.internal`

Примеры:
- `ru.max.botframework.dispatcher.internal.pipeline`
- `ru.max.botframework.client.internal.http`
- `ru.max.botframework.fsm.internal.storage`

Правила:
- Типы из `.internal` не являются частью public API.
- `.internal` может меняться без guarantees обратной совместимости.
- Публичные классы не должны возвращать internal-типы в сигнатурах.

## Naming rules: general

- Классы/интерфейсы: `PascalCase`.
- Методы/поля: `camelCase`.
- Константы: `UPPER_SNAKE_CASE`.
- Пакеты: lowercase, без underscore, без сокращений кроме общепринятых (`fsm`, `api`).
- Префикс `I` для интерфейсов запрещён.
- Суффиксы `Impl`, `Base` в public API запрещены; допустимы только во внутренней инфраструктуре.

## Naming rules: public API

### Core runtime types

- Оркестраторы и модели именуются существительными:
  - `Dispatcher`, `Router`, `Context`, `Update`.
- Типы действий именуются глагольными методами:
  - `send`, `edit`, `delete`, `reply`, `answerCallback`.

### Builders

- Builder-типы заканчиваются на `Builder`:
  - `MessageBuilder`, `KeyboardBuilder`, `AnswerCallbackBuilder`.
- Factory entrypoints — во множественном числе:
  - `Messages`, `Keyboards`, `Buttons`, `Callbacks`, `Uploads`.
- Fluent-методы — глагольные/атрибутивные:
  - `text(...)`, `format(...)`, `notify(...)`, `keyboard(...)`.

### Filters

- Базовый контракт: `Filter<C extends Context>`.
- DSL entrypoint: `F`.
- Предикатные методы называют условие, а не реализацию:
  - `startsWith`, `equalsTo`, `stateIs`, `stateIn`, `present`.
- Composite операции: `and`, `or`, `not`.

### Middleware

- Контракт: `Middleware`.
- Реализации имеют суффикс `Middleware`:
  - `LoggingMiddleware`, `AuthMiddleware`.
- API регистрации отражает scope:
  - `outerMiddleware(...)`, `innerMiddleware(...)`.

### Events and contexts

- Нормализованные события:
  - `Update`, `UpdateType`.
- Typed контексты:
  - `MessageContext`, `CallbackContext`.
- События и payload не используют transport-специфичные имена в public слое.

## Naming rules: internal infrastructure

- Internal реализации могут использовать суффиксы:
  - `Default*`, `*Resolver`, `*Registry`, `*Pipeline`, `*Adapter`.
- Internal DTO для transport:
  - `*Request`, `*Response`, `*Payload` в `.internal` пакетах.
- Внутренние factory/helper имена должны отражать техническую роль:
  - `HandlerInvoker`, `ArgumentResolver`, `UpdateNormalizer`.

## API stability rules

- Публичные имена, попавшие в docs/spec, считаются стабильными.
- Переименование public типов/методов — только с migration note.
- Internal naming может эволюционировать без обязательств по стабильности.

## Examples

### Good public naming

```java
Router router = new Router("main");
router.message(F.message().commandStart(), ctx -> ctx.reply("Hi"));
```

### Good internal naming

```java
package ru.max.botframework.dispatcher.internal.pipeline;

final class DefaultHandlerInvoker {
    // internal runtime glue
}
```

## Related docs

- [api-contract.md](api-contract.md)
- [product-spec.md](product-spec.md)
- [adr/0002-multi-module-structure.md](adr/0002-multi-module-structure.md)
- [adr/0003-client-runtime-separation.md](adr/0003-client-runtime-separation.md)
