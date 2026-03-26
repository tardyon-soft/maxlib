# Sprint 6 Messaging Examples

Примеры high-level messaging API.

## Файлы

- `MessagingFacadeExample.java`:
  - `send/edit/delete/reply`;
  - `plain/markdown/html` текст;
  - inline keyboard + callback кнопки;
  - callback answer через `CallbackFacade`;
  - chat actions через `ChatActionsFacade`.
- `RuntimeMessagingHandlersExample.java`:
  - `Dispatcher.withBotClient(...)`;
  - shortcuts в `RuntimeContext`: `reply`, `answerCallback`, `chatAction`;
  - DI параметров `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`.

## Актуальность

- Для callback-обновления текущего сообщения используйте `ctx.callbacks().updateCurrentMessage(...)` с реальным `callback.message().messageId()`.
- При отсутствии корректного `messageId` нужно fallback-поведение (send вместо edit/update).
