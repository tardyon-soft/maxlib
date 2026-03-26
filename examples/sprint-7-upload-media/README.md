# Sprint 7 Upload/Media Examples

Примеры upload/media API на runtime уровне.

## Файлы

- `MediaFacadeExample.java`:
  - `InputFile.fromPath(...)` и `InputFile.fromBytes(...)`;
  - `sendImage(...)`, `sendFile(...)`;
  - `replyVideo(...)`, `replyAudio(...)`;
  - message builder с media attachments.
- `RuntimeMediaHandlersExample.java`:
  - `Dispatcher.withBotClient(...).withUploadService(...)`;
  - `RuntimeContext` shortcuts (`replyImage`, `replyFile`, `sendVideo`, `sendAudio`);
  - DI `MediaMessagingFacade`.

## Актуальность

- Для реального MAX окружения используйте только валидные upload/video tokens.
- В screen API media тоже поддерживаются через widget attachments, но с тем же требованием к валидным reference.
