# Sprint 4 Filters/Middleware Examples

Минимальные примеры использования filters + middleware в текущем runtime API.

Файлы:
- `FiltersMiddlewareExample.java`:
  - регистрация handler с built-in filters;
  - композиция filters через `and`;
  - `outerMiddleware` на `Dispatcher`;
  - `innerMiddleware` на `Router`;
  - enrichment через `RuntimeContext.putEnrichment(...)` и downstream чтение через `enrichmentValue(...)`;
  - `router tree` через `includeRouter`.

Пример не использует не реализованные фичи (DI parameter injection, FSM/scenes, magic filter DSL).
