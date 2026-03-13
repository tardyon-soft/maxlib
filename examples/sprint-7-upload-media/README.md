# Sprint 7 Upload/Media Examples

Минимальные и реалистичные примеры использования upload/media API на текущем runtime.

Файлы:
- `MediaFacadeExample.java`:
  - `InputFile.fromPath(...)` и `InputFile.fromBytes(...)`;
  - `sendImage(...)` и `sendFile(...)`;
  - `replyVideo(...)` и `replyAudio(...)`;
  - builder + media attachment composition (`Messages.text(...).attachment(MediaAttachment...)`).
- `RuntimeMediaHandlersExample.java`:
  - интеграция `Dispatcher.withBotClient(...).withUploadService(...)`;
  - `RuntimeContext` shortcuts (`replyImage`, `replyFile`, `sendVideo`, `sendAudio`);
  - reflective handler parameter resolution `MediaMessagingFacade`.

Ограничения:
- examples не показывают Spring starter;
- examples не используют FSM/scenes;
- examples не используют вымышленные storage adapters/hooks.
