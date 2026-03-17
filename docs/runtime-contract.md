# Runtime Contract

## Status

Документ фиксирует поведение runtime слоя (`max-dispatcher`) в текущей реализации.

## Dispatcher lifecycle

- `Dispatcher` создаётся без обязательных внешних зависимостей.
- Runtime функции (messaging, FSM, scenes, upload) активируются только при явном wiring (`with*` methods).
- `feedUpdate(Update)` — основной entrypoint.

## Router graph

- В dispatcher можно включать только root routers (`router.parent().isEmpty()`).
- `Router.includeRouter(child)` строит дерево.
- Запрещены self-include, циклы и повторный parent.

## Dispatch order

1. Outer middleware chain (dispatcher-level)
2. Root routers в порядке `includeRouter(...)`
3. Для каждого root: DFS preorder по router subtree
4. Для каждого router:
   - update observer
   - resolved observer (`message` или `callback`) по `UpdateEventResolver`

## Handler execution

- Filter match required.
- Inner middleware выполняется вокруг matched handler.
- Reflective/lambda handlers унифицированы до `EventHandler<TEvent>`.
- Успешный handler -> `DispatchStatus.HANDLED`.
- Miss -> `DispatchStatus.IGNORED`.
- Ошибка -> `DispatchStatus.FAILED` + error observer flow.

## Error boundary

- Ошибки filter/middleware/invocation классифицируются в `RuntimeDispatchErrorType`.
- Router-level `error(...)` handlers получают `ErrorEvent`.
- Если error handler тоже падает, suppression добавляется к исходной ошибке.

## RuntimeContext guarantees

`RuntimeContext` request-scoped и содержит:

- исходный `Update`
- enrichment (`FILTER` + `MIDDLEWARE` scopes)
- typed runtime data container
- shortcuts: `reply`, `answerCallback`, `chatAction`, `media`, `fsm`, `scenes`, `wizard`

Conflict policy:

- enrichment key/type конфликты приводят к runtime error (`EnrichmentConflictException`).
