# ADR-0001: Router Model and Handler Resolution

- Status: Accepted
- Date: 2026-03-12

## Context

Framework должен предоставить предсказуемую и расширяемую routing-модель для `Dispatcher`/`Router`, совместимую с event model MVP (`MESSAGE`, `CALLBACK`, `UNKNOWN`) и одинаково работающую для polling и webhook.

Нужно зафиксировать:
- как устроен `Router`;
- как работает `includeRouter`;
- как выбирается handler;
- что такое observer и как он связан с `UpdateType`;
- как `Dispatcher` взаимодействует с графом router-ов.

## Decision

### 1. Router model

`Router` — именованный узел дерева маршрутизации со следующими logical частями:
- `children`: включённые дочерние router-ы в порядке `includeRouter`;
- `observers`: typed observers по `UpdateType`;
- `innerMiddleware`: middleware, применяемые к matched handler в границах router-а.

Для MVP используется observer набор:
- `message` observer для `UpdateType.MESSAGE`;
- `callback` observer для `UpdateType.CALLBACK`.

`UpdateType.UNKNOWN` не имеет business observer и не маршрутизируется в handlers.

### 2. includeRouter semantics

`includeRouter(child)`:
- включает дочерний router по ссылке (без копирования handlers);
- сохраняет insertion order;
- возвращает родительский router для fluent API.

Ограничения:
- self-include запрещён;
- циклы в графе запрещены;
- один и тот же instance router-а нельзя включить повторно в тот же parent.

Нарушение ограничений приводит к конфигурационной ошибке (fail-fast при сборке graph).

### 3. Observer model

Observer — это typed registry handler-ов для конкретного `UpdateType`.

Каждая запись observer-а (`RouteEntry`) содержит:
- `filter` (или compose filters);
- `handler`;
- metadata (например, route id/name);
- optional route-level middleware references (post-MVP extension).

Порядок route entries фиксируется порядком регистрации.

### 4. Handler resolution algorithm

Для каждого normalized `Update` используется deterministic алгоритм:

1. `Dispatcher` выбирает корневые router-ы в порядке `dispatcher.includeRouter(...)`.
2. Выполняется обход router graph в pre-order DFS:
- сначала текущий router;
- затем его children по порядку include.
3. Для текущего router выбирается observer по `UpdateType`.
4. В observer записи проверяются по порядку регистрации.
5. Первая запись, у которой filter вернул `true`, считается matched route.
6. Для matched route выполняется pipeline:
- router-scoped inner middleware chain;
- handler invocation с context injection.
7. После успешного handler invocation обработка завершается (`first-match-wins`).
8. Если матчей нет, поиск продолжается дальше по DFS.
9. Если матчей нет во всём graph, update считается unhandled.

### 5. Dispatcher <-> Router relation

`Dispatcher` — владелец root router list и global pipeline:
- outer middleware (global);
- error boundary;
- delegation в router graph resolver.

`Router` не запускает runtime самостоятельно и не зависит от transport.

Оба transport-а (polling/webhook) обязаны вызывать один и тот же entrypoint dispatcher-а, поэтому routing semantics идентичны независимо от источника update.

## Consequences

Плюсы:
- предсказуемый и легко объяснимый порядок маршрутизации;
- минимум скрытой магии для пользователя;
- простая отладка: всегда понятно, почему сработал конкретный handler;
- удобный путь к расширениям (priorities, route flags, scene-aware observers).

Минусы/ограничения MVP:
- по умолчанию исполняется только один handler на update (`first-match-wins`);
- fan-out режим (вызов нескольких handler-ов) не поддерживается;
- router graph intentionally строгий (cycle/self include fail-fast).

## Alternatives considered

1. Broadcast (вызывать все matched handlers):
- отклонено для MVP из-за неочевидных сайд-эффектов и сложной идемпотентности.

2. Breadth-first traversal:
- отклонено, так как хуже для модульной локальности (parent-first DSL читается как DFS/pre-order).

3. Разрешить циклы и runtime detection:
- отклонено, fail-fast в конфигурации проще и безопаснее.

## Follow-ups

- При необходимости добавить configurable propagation policy (`first-match` / `continue`).
- Зафиксировать formal pseudo-code resolver-а в runtime spec.
- Добавить testkit fixtures для проверки routing order и includeRouter ограничений.

## References

- [../event-model.md](../event-model.md)
- [../api-contract.md](../api-contract.md)
- [../product-spec.md](../product-spec.md)
