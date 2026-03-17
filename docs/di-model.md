# DI Model

## Status

Документ отражает текущую runtime DI модель в `DefaultHandlerInvoker`.

## Resolution sources (effective)

Параметры reflective handler-методов резолвятся через registry resolver-ов:

1. RuntimeContext/FSM/Scene/Wizard abstractions
2. Event payload и related objects (`Update`, `Message`, `Callback`, `User`, `Chat`)
3. Filter-produced data (по уникальному типу)
4. Middleware-produced data (по уникальному типу)
5. Application data/services (`Dispatcher.registerService/registerApplicationData`)

## Matching rules

- Основное правило: match по типу параметра.
- Если в одном source несколько кандидатов того же типа -> ambiguous resolution error.
- Если параметр не резолвится -> missing/unsupported parameter error.

## Supported handler styles

- lambda handlers (`EventHandler`, `ContextualEventHandler`)
- reflective handlers (`target + Method`)

## Return type rules

Reflective handler method должен вернуть:

- `void` или
- `CompletionStage<?>`

Иные return types считаются invocation error.

## About parameter annotations

В текущей реализации нет runtime-аннотаций вида `@FromFilter/@FromContext`.
Резолв выполняется по типам и resolver pipeline.
