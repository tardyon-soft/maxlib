# Sprint 5 DI/Invocation Examples

Примеры резолвинга параметров handler-методов.

## Файл

- `HandlerDiExample.java`:
  - runtime/update параметры (`Message`, `Update`, `User`, `Chat`);
  - filter-derived аргументы (например suffix из `BuiltInFilters.textStartsWith(...)`);
  - middleware-derived enrichment (`RuntimeContext.putEnrichment(...)`);
  - custom shared services (`Dispatcher.registerService(...)`);
  - combined pipeline: `outer middleware -> filters -> invocation`.

## Актуальность

- Поддерживаемые return-типы handler: `void` и `CompletionStage<?>`.
- Пример отражает core runtime API без Spring-specific auto-configuration.
