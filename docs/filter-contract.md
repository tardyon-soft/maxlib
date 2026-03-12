# Filter Contract Specification

## Status

Этот документ фиксирует контракт filter subsystem для framework MVP.
Это спецификация публичного API и поведения, а не реализация.

## Goals

- Дать предсказуемый и композиционный filter API.
- Обеспечить удобный DSL для `message` и `callback` routing.
- Зафиксировать влияние filter match на `Context` и dependency injection.

## Base public interface

Минимальный контракт фильтра:

```java
@FunctionalInterface
public interface Filter<C extends Context> {
    FilterResult test(C ctx);

    default Filter<C> and(Filter<? super C> other) { ... }
    default Filter<C> or(Filter<? super C> other) { ... }
    default Filter<C> not() { ... }
}
```

Результат проверки:

```java
public sealed interface FilterResult permits Matched, NotMatched {}

public record Matched(FilterBindings bindings) implements FilterResult {}
public record NotMatched() implements FilterResult {}
```

Где `FilterBindings` — typed key-value map для данных, извлечённых фильтром (например, parsed command args / callback payload fields).

## MVP built-in filters

### Message filters (MVP)

- `F.message().any()`
- `F.message().text().present()`
- `F.message().text().equalsTo(String value)`
- `F.message().text().startsWith(String prefix)`
- `F.message().command(String command)`
- `F.message().commandStart()` (shortcut for `/start`)
- `F.message().chat().type().isPrivate()`
- `F.message().chat().type().isGroup()`

### Callback filters (MVP)

- `F.callback().any()`
- `F.callback().data().present()`
- `F.callback().data().equalsTo(String value)`
- `F.callback().data().startsWith(String prefix)`

### Cross-cutting filters (MVP)

- `F.stateIs(String state)` — интеграция с `FSMContext`.
- `F.custom(Filter<C>)` — явное подключение пользовательского фильтра.

Детальный контракт `StateFilter`, scopes и интеграции со сценами:
- [fsm-scenes-contract.md](fsm-scenes-contract.md)

## Filter composition rules

### `and`

- Выполняется слева направо.
- Short-circuit: если левый фильтр не матчится, правый не выполняется.
- Если оба матчатся, bindings объединяются.

Правило merge bindings:
- разные ключи: объединяются;
- одинаковый ключ с одинаковым значением: допускается;
- одинаковый ключ с разными значениями: результат `NotMatched` (fail-safe против двусмысленности).

### `or`

- Выполняется слева направо.
- Short-circuit: если левый фильтр матчится, правый не выполняется.
- Возвращаются bindings только с победившей ветки.

### `not`

- Инвертирует match-семантику исходного фильтра.
- `not` не экспортирует bindings внутреннего фильтра (чтобы избежать неочевидных контрактов данных).

### Purity/side-effects contract

Фильтры должны быть:
- детерминированными относительно входного `Context`;
- без сетевых/блокирующих I/O операций;
- без внешних побочных эффектов.

## Filter bindings and Context/DI

### Binding export contract

Если фильтр извлекает структурированные данные, он экспортирует их в `FilterBindings`.

Пример: callback filter извлекает `orderId` из `order:42`.

```java
FilterKey<Long> ORDER_ID = FilterKey.of("orderId", Long.class);

router.callback(
    F.callback().data().pattern("order:(\\d+)", ORDER_ID, Long::parseLong),
    (CallbackContext ctx, @FromFilter(ORDER_ID) Long orderId) -> {
        ctx.answerCallback("Order: " + orderId);
    }
);
```

### Interaction with Context

- `Context` предоставляет доступ к bindings текущего matched route.
- Bindings живут только в рамках текущего update processing и не персистятся автоматически.

Пример API:

```java
Long orderId = ctx.filters().get(ORDER_ID).orElseThrow();
```

### Interaction with dependency injection

В handler injection разрешены:
- объекты update/context уровня;
- сервисы из DI container;
- значения из filter bindings (typed).

Resolution order (MVP):
1. framework-provided context/update objects;
2. filter bindings;
3. application DI container.

Если обязательный параметр `@FromFilter` не найден в bindings, handler не вызывается и считается route mismatch (с диагностикой в debug log).

## Error and diagnostics contract

- Ошибки парсинга в built-in extracting filters приводят к `NotMatched`, а не к исключению в handler pipeline.
- Неправильная конфигурация фильтра (например, invalid regex в bootstrap) должна падать fail-fast на этапе регистрации.
- Debug tooling/testkit должен уметь показать, какой filter в цепочке дал `NotMatched`.

## Examples

### Basic composition

```java
router.message(
    F.message().text().present()
        .and(F.message().chat().type().isPrivate())
        .and(F.message().command("start")),
    ctx -> ctx.reply("Welcome")
);
```

### Callback prefix filter

```java
router.callback(
    F.callback().data().startsWith("pay:"),
    ctx -> ctx.answerCallback("Processing payment")
);
```

## Out of scope (for this document)

- Полный каталог всех post-MVP built-in filters.
- Внутренняя реализация DSL parser/combinators.
- Runtime performance optimizations filter evaluation.

## Related docs

- [api-contract.md](api-contract.md)
- [event-model.md](event-model.md)
- [adr/0001-router-model.md](adr/0001-router-model.md)
