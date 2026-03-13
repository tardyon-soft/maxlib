# Upload And Media Contract (Sprint 7)

## Status

Документ фиксирует contract upload/media layer для Sprint 7.
Это спецификация public API и runtime boundaries, а не реализация.

Состояние реализации Sprint 7.1.2:
- реализован `InputFile` (`fromPath`, `fromBytes`, `fromStream`) в `ru.max.botframework.upload`.

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
- `InputFile.fromExistingRef(UploadRef ref)` для повторного использования уже загруженного файла.

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

### Upload Result Model

`UploadResult` возвращается `UploadService` и содержит минимум:
- `UploadRef ref` — reference для дальнейшей отправки;
- `UploadMetadata` (optional: file id, size, mime type, checksum);
- `UploadFlowType` (`MULTIPART` / `RESUMABLE`);
- `stage diagnostics` для observability/testkit.

### Media Attachment Abstractions

High-level typed media attachments:
- `MediaAttachment.image(InputFile input)`;
- `MediaAttachment.file(InputFile input)`;
- `MediaAttachment.video(InputFile input)`;
- `MediaAttachment.audio(InputFile input)`.

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

Примерный contract:
- `media.send(MessageTarget, MediaMessageBuilder)`;
- `media.reply(Message, MediaMessageBuilder)`;
- mapping в существующие message endpoints через ранее введённые DTO/request builders.

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
media.send(
    MessageTarget.chat(chatId),
    MediaMessages.image(InputFile.fromPath(Path.of("./photo.jpg")))
        .caption("Фото")
);

media.send(
    MessageTarget.chat(chatId),
    MediaMessages.file(InputFile.fromBytes(bytes, "doc.pdf"))
        .caption("Документ")
);
```

### 3) Reply with media

```java
media.reply(
    sourceMessage,
    MediaMessages.video(InputFile.fromPath(Path.of("./clip.mp4")))
        .caption("Видео-ответ")
);
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
