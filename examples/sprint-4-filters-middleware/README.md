# Sprint 4 Filters/Middleware Examples

Примеры filters + middleware в runtime API.

## Файл

- `FiltersMiddlewareExample.java`:
  - built-in filters и композиция через `and`;
  - `outerMiddleware` на `Dispatcher`;
  - `innerMiddleware` на `Router`;
  - enrichment через `RuntimeContext.putEnrichment(...)` и чтение в handler;
  - `router tree` через `includeRouter`.

## Актуальность

- Пример актуален для текущего pipeline.
- Командные фильтры (`BuiltInFilters.command(...)`) имеют повышенный приоритет регистрации относительно generic text/state filters.
