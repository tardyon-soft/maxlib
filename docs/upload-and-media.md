# Upload And Media Contract (Sprint 7)

## Status

Документ фиксирует contract upload/media layer для Sprint 7.
Это спецификация public API и runtime boundaries, а не реализация.

Состояние реализации Sprint 7.1.2:
- реализован `InputFile` (`fromPath`, `fromBytes`, `fromStream`) в `ru.tardyon.botframework.upload`.

Состояние реализации Sprint 7.1.3:
- реализован orchestration contract в `ru.tardyon.botframework.upload`:
  - `UploadService` / `DefaultUploadService`;
  - `UploadPreparationGateway` (prepare stage, `POST /uploads`);
  - `UploadTransferGateway` (raw upload URL transfer);
  - `UploadFinalizeGateway` (finalize stage);
  - `UploadResultMapper` / `DefaultUploadResultMapper`.
- добавлены модели orchestration result chain:
  `UploadPrepareCommand`, `UploadPreparation`, `UploadTransferReceipt`, `UploadFinalizeResult`, `UploadResult`, `UploadRef`.

Состояние реализации Sprint 7.2.1:
- реализован multipart transfer path:
  - `MultipartUploadTransferGateway`;
  - `MultipartUploadHttpClient` + `JdkMultipartUploadHttpClient`;
  - `MultipartUploadRequest` / `MultipartUploadResponse`.

Состояние реализации Sprint 7.2.2:
- реализован resumable transfer path:
  - `ResumableUploadTransferGateway`;
  - `ResumableChunkUploadClient`;
  - `ResumableChunkUploadRequest` / `ResumableChunkUploadResponse`;
  - `ResumableUploadOptions` (`chunkSizeBytes`, `maxRetriesPerChunk`).

Состояние реализации Sprint 7.2.3:
- унифицирован upload result model:
  - `UploadFinalizeResult` расширен `mediaKind` и `attachmentPayload`;
  - единый `UploadResult` теперь содержит `mediaKind` и immutable `attachmentPayload`;
  - `DefaultUploadResultMapper` нормализует результаты multipart/resumable в один contract.

Состояние реализации Sprint 7.3.1:
- добавлены high-level media attachment abstractions в `ru.tardyon.botframework.message`:
  - `ImageAttachment`, `FileAttachment`, `VideoAttachment`, `AudioAttachment`;
  - общий контракт `MediaAttachment`;
  - mapping в существующий low-level `NewMessageAttachment` (`AttachmentInput.uploadRef`).
- `MessageBuilder` расширен overload-методом `attachment(MediaAttachment)`.

Состояние реализации Sprint 7.3.2:
- добавлен high-level media send/reply facade `MediaMessagingFacade`:
  - `sendImage/sendFile/sendVideo/sendAudio`;
  - `replyImage/replyFile/replyVideo/replyAudio`.
- реализация переиспользует существующие слои:
  - `InputFile`;
  - `UploadService`;
  - `MessagingFacade`.

Состояние реализации Sprint 7.3.3:
- добавлен token-aware behavior для video/audio:
  - `UploadResult.mediaTokenOptional()` извлекает token из normalized payload;
  - поддерживаемые payload keys: `videoToken`, `audioToken`, fallback `token`;
  - `VideoAttachment`/`AudioAttachment` используют token как attachment reference
    (fallback на `uploadRef`, если token отсутствует).
- image/file flows остаются uploadRef-oriented.
- read-model helper для `GET /videos/{videoToken}` не добавлялся в текущем scope.

Состояние реализации Sprint 7.3.4:
- media layer интегрирован в runtime ergonomics:
  - `Dispatcher.withUploadService(...)`;
  - `RuntimeContext.media()`;
  - runtime shortcuts: `replyImage/replyFile/sendVideo/sendAudio`.
- `MediaMessagingFacade` добавлен в built-in parameter resolution для reflective handlers.
- composition через `MessageBuilder + MediaAttachment` подтверждена integration-пайплайном dispatcher runtime.

Состояние реализации Sprint 7.4.1:
- добавлен regression safety net для upload/media:
  - unit coverage: `InputFile`, orchestration, multipart, resumable, result normalization,
    attachment mapping, token-aware video/audio behavior;
  - integration coverage: `upload -> send/reply`, `runtime handler -> media API`,
    builder/media composition scenarios.

## Goal

Дать разработчику ergonomic API для отправки медиа в стиле framework-level DX:
- загрузка из `path/bytes/stream`;
- отправка `image/file/video/audio`;
- `reply` с медиа;
- без ручного управления многошаговым upload flow.

## Scope (Sprint 7)

В scope:
- `InputFile` abstraction;
- `UploadService`;
- orchestration для multipart/resumable upload;
- upload result model;
- media attachment abstractions;
- high-level media send/reply facade поверх существующего messaging layer.

Out of scope:
- storage subsystem;
- CDN/file hosting abstraction;
- Spring Boot starter;
- FSM/scenes.

## Core Roles And Boundaries

### `InputFile`

Назначение: единая типобезопасная абстракция входного медиа-источника.

Поддерживаемые формы (MVP):
- `InputFile.fromPath(Path path)`;
- `InputFile.fromBytes(byte[] bytes, String fileName)`;
- `InputFile.fromStream(StreamSupplier stream, String fileName)`;

Границы:
- не содержит HTTP/transport деталей;
- не управляет ретраями/таймаутами;
- может содержать metadata (`fileName`, optional `contentType`, optional `size`).

### `UploadService`

Назначение: orchestration upload pipeline поверх существующего SDK transport.

Ответственность:
- определить стратегию upload (multipart vs resumable) на основе доступных MAX endpoints/ограничений;
- выполнить этапы `prepare -> transfer -> finalize`;
- вернуть типизированный `UploadResult`.

Границы:
- не отправляет сообщение напрямую;
- не дублирует `MaxBotClient` transport/client stack;
- не хранит файл-персистентность вне жизненного цикла операции.

Текущая реализация Sprint 7.1.3:
- `DefaultUploadService` orchestrates `prepare -> transfer -> finalize -> map`;
- `UploadService.of(...)` даёт базовую wiring-точку с default mapper;
- transport реализации gateways и media façade интеграция добавлены в Sprint 7.2-7.3.

### Multipart Upload Flow

Используется для небольших/простых upload-сценариев, где endpoint принимает single-shot transfer.

Contract-level этапы:
1. `prepare` (получить upload metadata/target).
2. `transfer` (multipart body upload).
3. `finalize` (получить reference для message attachment).

### Resumable Upload Flow

Используется для больших файлов/частичных повторов, если MAX API это поддерживает для конкретного media surface.

Contract-level этапы:
1. `prepare` (инициализация upload session).
2. `transferChunks` (последовательная отправка chunk-ов).
3. `finalize` (закрытие сессии и получение итогового reference).

Важно:
- это strategy-level abstraction;
- если resumable недоступен для конкретного endpoint, `UploadService` использует multipart flow.
- текущая реализация хранит resumable state только в рамках одной runtime операции
  (current committed offset) и не использует distributed/persistent resume storage.
- retry semantics ограничены chunk-step уровнем и управляются `ResumableUploadOptions`.

### Upload Result Model

`UploadResult` возвращается `UploadService` и содержит:
- `UploadRef ref` — reference для дальнейшей отправки;
- `UploadFlowType` (`MULTIPART` / `RESUMABLE`);
- `bytesTransferred`, optional `contentType`;
- `UploadMediaKind` (`IMAGE`, `FILE`, `VIDEO`, `AUDIO`, `UNKNOWN`);
- immutable `attachmentPayload` для media-specific metadata (например, width/height/duration/preview refs).

Границы:
- результат скрывает raw transport-level детали (HTTP status, chunk protocol internals);
- модель остаётся attachment-oriented и пригодной для high-level media builders.

### Media Attachment Abstractions

High-level typed media attachments:
- `MediaAttachment.image(UploadResult uploaded)`;
- `MediaAttachment.file(UploadResult uploaded)`;
- `MediaAttachment.video(UploadResult uploaded)`;
- `MediaAttachment.audio(UploadResult uploaded)`.

Optional common options:
- `caption`;
- `format`;
- `notify`;
- keyboard integration через существующий `MessageBuilder`.

Границы:
- media abstractions выражают доменную модель отправки;
- upload orchestration остаётся в `UploadService`.

### High-level Media Send/Reply Facade

Назначение: ergonomic API поверх уже существующих `MessagingFacade` + `UploadService`.

Текущий contract (`MediaMessagingFacade`):
- `sendImage/sendFile/sendVideo/sendAudio`;
- `replyImage/replyFile/replyVideo/replyAudio`;
- mapping в существующие message endpoints через `MessageBuilder + NewMessageAttachment`.

Границы:
- не дублирует низкоуровневые SDK clients;
- reuse существующего messaging pipeline (`send/edit/reply` semantics, context integration).

## Desired Developer Experience

### 1) Upload from path/bytes/stream

```java
InputFile pathFile = InputFile.fromPath(Path.of("./invoice.png"));
InputFile bytesFile = InputFile.fromBytes(bytes, "report.pdf");
InputFile streamFile = InputFile.fromStream(() -> inputStream, "voice.ogg");
```

### 2) Send image/file/video/audio

```java
media.sendImage(MessageTarget.chat(chatId), InputFile.fromPath(Path.of("./photo.jpg")));
media.sendFile(MessageTarget.chat(chatId), InputFile.fromBytes(bytes, "doc.pdf"), "Документ");
```

### 3) Reply with media

```java
media.replyVideo(sourceMessage, InputFile.fromPath(Path.of("./clip.mp4")), "Видео-ответ");
media.replyAudio(sourceMessage, InputFile.fromBytes(audioBytes, "voice.mp3"), "Аудио-ответ");
```

## Error Boundaries

Минимальные категории ошибок:
- `UploadValidationException` (invalid `InputFile` / unsupported media options);
- `UploadTransferException` (transfer stage failure);
- `UploadFinalizeException` (finalize stage failure);
- `MediaSendException` (message send mapping/API failure after successful upload).

Требования:
- ошибка должна включать stage (`prepare/transfer/finalize/send`);
- transport/API errors reuse существующую SDK error model.

## Integration Constraints

- Sprint 7 reuse уже существующий SDK (`MaxBotClient`, transport/auth/serialization/error stack).
- Sprint 7 reuse уже существующий messaging/runtime слой (`MessageBuilder`, `MessagingFacade`, `RuntimeContext` integration).
- Upload/media контракт не должен ломать текущий Sprint 6 public API.

## Related docs

- [api-contract.md](api-contract.md)
- [messaging-api.md](messaging-api.md)
- [upload-media-contract.md](upload-media-contract.md)
- [roadmap.md](roadmap.md)
