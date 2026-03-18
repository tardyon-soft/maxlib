# Messaging API

## Scope

Документ описывает runtime messaging слой в `max-dispatcher`.

## Facades

- `MessagingFacade`: send/edit/delete/reply сообщений
- `CallbackFacade`: answer callback/update callback message
- `ChatActionsFacade`: chat actions (`typing_on`, etc.)
- `MediaMessagingFacade`: media send/reply (если подключен upload service)

## Builders

- `Messages` / `MessageBuilder`
- `Buttons` / `Keyboards` / `KeyboardBuilder`
- `CallbackAnswers` / `CallbackAnswerBuilder`

## RuntimeContext shortcuts

- `reply(...)`
- `answerCallback(...)`
- `chatAction(...)`
- `media()` + media convenience methods

## Integration requirements

- `Dispatcher.withBotClient(...)` обязателен для messaging/callback/actions.
- `Dispatcher.withUploadService(...)` нужен для media shortcuts.
