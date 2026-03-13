# Sprint 6 Messaging Examples

Минимальные и реалистичные примеры использования high-level messaging API.

Файлы:
- `MessagingFacadeExample.java`:
  - `send/edit/delete/reply`;
  - formatted text (`plain/markdown/html`);
  - keyboard builder + typed buttons;
  - callback answer через `CallbackFacade`;
  - chat actions через `ChatActionsFacade`.
- `RuntimeMessagingHandlersExample.java`:
  - использование `Dispatcher.withBotClient(...)`;
  - handler ergonomics через `RuntimeContext` (`reply`, `answerCallback`, `chatAction`);
  - parameter resolution для `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`.

Ограничения:
- examples не показывают upload/media flow;
- examples не используют FSM/scenes;
- примеры используют только фактический API текущего Sprint 6.
