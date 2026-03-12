# Sprint 5 DI/Invocation Examples

Минимальные примеры использования handler parameter resolution в текущем runtime API.

Файл:
- `HandlerDiExample.java`:
  - handler с core runtime/update параметрами (`Message`, `Update`, `User`, `Chat`);
  - handler с filter-derived параметром (`String` suffix из `BuiltInFilters.textStartsWith("pay:")`);
  - handler с middleware-derived параметром (`Integer` из `RuntimeContext.putEnrichment(...)`);
  - handler с custom shared service (`Dispatcher.registerService(...)`);
  - combined pipeline `outer middleware -> filters -> invocation` с mixed параметрами.

Пример использует только фактический текущий API (`Dispatcher`, `Router`, reflective method handlers, built-in resolvers),
без Spring/FSM/scenes и без несуществующих аннотаций/DSL.
