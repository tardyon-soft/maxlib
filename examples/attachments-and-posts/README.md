# Attachment and post examples

Собранные примеры отправки attachments, списка attachments, публикации постов, редактирования постов и отслеживания событий публикации/редактирования.

Полный Java-файл с теми же примерами: `AttachmentAndPostExamples.java`.

## Базовая инициализация

```java
MaxBotClient botClient = createConfiguredBotClient();
MessagingFacade messaging = new MessagingFacade(botClient);
ChatId chatId = new ChatId("chat-or-channel-id");
MessageTarget target = MessageTarget.chat(chatId);
```

В примерах ниже:

```java
private static AttachmentInput uploadToken(String token) {
    return new AttachmentInput(null, token, null);
}

private static AttachmentInput fileId(String value) {
    return new AttachmentInput(new FileId(value), null, null);
}

private static NewMessageAttachment media(
        MessageAttachmentType type,
        AttachmentInput input,
        String caption
) {
    return NewMessageAttachment.media(type, input, caption, null, null);
}
```

## IMAGE по URL

```java
messaging.send(target, Messages.text("IMAGE by URL")
        .attachment(NewMessageAttachment.imageUrl("https://example.com/image.png")));
```

## IMAGE по upload token

```java
messaging.send(target, Messages.text("IMAGE by upload token")
        .attachment(NewMessageAttachment.imageToken("uploaded-image-token")));
```

## PHOTO

```java
messaging.send(target, Messages.text("PHOTO by upload token")
        .attachment(media(
                MessageAttachmentType.PHOTO,
                uploadToken("uploaded-photo-token"),
                "Photo caption"
        )));
```

## VIDEO

```java
messaging.send(target, Messages.text("VIDEO by upload token")
        .attachment(media(
                MessageAttachmentType.VIDEO,
                uploadToken("uploaded-video-token"),
                "Video caption"
        )));
```

## AUDIO

```java
messaging.send(target, Messages.text("AUDIO by upload token")
        .attachment(media(
                MessageAttachmentType.AUDIO,
                uploadToken("uploaded-audio-token"),
                "Audio caption"
        )));
```

## DOCUMENT

```java
messaging.send(target, Messages.text("DOCUMENT by file id")
        .attachment(media(
                MessageAttachmentType.DOCUMENT,
                fileId("document-file-id"),
                "Document caption"
        )));
```

## FILE

```java
messaging.send(target, Messages.text("FILE by upload token")
        .attachment(media(
                MessageAttachmentType.FILE,
                uploadToken("uploaded-file-token"),
                "File caption"
        )));
```

## STICKER

```java
messaging.send(target, Messages.text("STICKER")
        .attachment(NewMessageAttachment.sticker("sticker-code")));
```

## LOCATION

```java
messaging.send(target, Messages.text("LOCATION")
        .attachment(NewMessageAttachment.location(55.751244, 37.618423)));
```

## SHARE по URL

```java
messaging.send(target, Messages.text("SHARE by URL")
        .attachment(NewMessageAttachment.shareUrl("https://example.com/post/123")));
```

## SHARE по token

```java
messaging.send(target, Messages.text("SHARE by token")
        .attachment(NewMessageAttachment.shareToken("shared-attachment-token")));
```

## INLINE_KEYBOARD

```java
messaging.send(target, Messages.text("INLINE_KEYBOARD")
        .keyboard(Keyboards.inline(k -> k.row(
                Buttons.callback("Edit post", "post:edit"),
                Buttons.link("Open", "https://example.com")
        ))));
```

## Список attachments в одном сообщении

```java
List<NewMessageAttachment> attachments = List.of(
        NewMessageAttachment.imageUrl("https://example.com/cover.png"),
        media(MessageAttachmentType.VIDEO, uploadToken("uploaded-video-token"), "Demo video"),
        media(MessageAttachmentType.FILE, fileId("release-notes-file-id"), "Release notes"),
        NewMessageAttachment.location(55.751244, 37.618423),
        NewMessageAttachment.shareUrl("https://example.com/release")
);

Message sent = messaging.send(chatId, Messages.markdown("""
        *Release post*

        Attachments are sent as one list in the message body.
        """).attachments(attachments));
```

## Отправить от 1 до 5 attachments разных типов

Соберите `List<NewMessageAttachment>` динамически и передайте его в `Messages...attachments(...)`. Тип каждого элемента может быть разным.

```java
List<NewMessageAttachment> attachments = new ArrayList<>();

attachments.add(NewMessageAttachment.imageUrl("https://example.com/cover.png"));

attachments.add(NewMessageAttachment.media(
        MessageAttachmentType.FILE,
        new AttachmentInput(null, "uploaded-file-token", null),
        "Документ",
        "application/pdf",
        null
));

attachments.add(NewMessageAttachment.location(55.751244, 37.618423));

attachments.add(NewMessageAttachment.shareUrl("https://example.com/post/123"));

attachments.add(NewMessageAttachment.sticker("sticker-code"));

if (attachments.isEmpty() || attachments.size() > 5) {
    throw new IllegalArgumentException("attachments count must be from 1 to 5");
}

Message sent = messaging.send(chatId, Messages.markdown("""
        *Сообщение с вложениями*

        В одном сообщении можно собрать attachments разных типов.
        """).attachments(attachments));
```

Если список строится из входных данных, удобно мапить свой тип attachment в SDK-модель:

```java
NewMessageAttachment toAttachment(DraftAttachment draft) {
    return switch (draft.type()) {
        case IMAGE_URL -> NewMessageAttachment.imageUrl(draft.url());
        case FILE_TOKEN -> NewMessageAttachment.media(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, draft.token(), null),
                draft.caption(),
                draft.mimeType(),
                draft.size()
        );
        case VIDEO_TOKEN -> NewMessageAttachment.media(
                MessageAttachmentType.VIDEO,
                new AttachmentInput(null, draft.token(), null),
                draft.caption(),
                draft.mimeType(),
                draft.size()
        );
        case LOCATION -> NewMessageAttachment.location(draft.latitude(), draft.longitude());
        case SHARE_URL -> NewMessageAttachment.shareUrl(draft.url());
        case STICKER -> NewMessageAttachment.sticker(draft.code());
    };
}

List<NewMessageAttachment> attachments = drafts.stream()
        .limit(5)
        .map(this::toAttachment)
        .toList();

if (attachments.isEmpty()) {
    throw new IllegalArgumentException("at least one attachment is required");
}

Message sent = messaging.send(chatId, Messages.text("Готово").attachments(attachments));
```

## Получить token/ref из локального файла

`UploadService.upload(...)` загружает файл и возвращает `UploadResult`. Для большинства attachments используйте `result.ref().value()` как upload token.

```java
UploadResult result = uploadService
        .upload(InputFile.fromPath(Path.of("./assets/report.pdf"))
                .withContentType("application/pdf"))
        .toCompletableFuture()
        .join();

String token = result.ref().value();

Message sent = messaging.send(chatId, Messages.text("Файл загружен")
        .attachment(NewMessageAttachment.media(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, token, null),
                "report.pdf",
                result.contentTypeOptional().orElse(null),
                result.bytesTransferred()
        )));
```

## Получить token/ref из byte array

```java
byte[] bytes = loadPdfBytes();

UploadResult result = uploadService
        .upload(InputFile.fromBytes(bytes, "invoice.pdf")
                .withContentType("application/pdf"))
        .toCompletableFuture()
        .join();

String token = result.ref().value();
```

## Получить token/ref из ссылки на файл

Если нужна именно загрузка в MAX и получение token/ref, ссылку надо открыть как stream и передать в `InputFile.fromStream(...)`.

```java
URI fileUri = URI.create("https://example.com/files/report.pdf");

InputFile remoteFile = InputFile
        .fromStream(() -> fileUri.toURL().openStream(), "report.pdf")
        .withContentType("application/pdf");

UploadResult result = uploadService.upload(remoteFile)
        .toCompletableFuture()
        .join();

String token = result.ref().value();
```

Для больших файлов лучше передать известный размер:

```java
long size = resolveRemoteContentLength(fileUri);

InputFile remoteFile = InputFile
        .fromStream(() -> fileUri.toURL().openStream(), "report.pdf", size)
        .withContentType("application/pdf");
```

## Token для VIDEO/AUDIO

Для video/audio API может вернуть отдельный media token. В этом случае используйте `mediaTokenOptional()`, а если его нет, fallback на `ref()`.

```java
UploadResult result = uploadService
        .upload(
                InputFile.fromPath(Path.of("./assets/clip.mp4"))
                        .withContentType("video/mp4"),
                UploadRequest.defaults().withMediaTypeHint("video")
        )
        .toCompletableFuture()
        .join();

String token = result.mediaTokenOptional().orElse(result.ref().value());

Message sent = messaging.send(chatId, Messages.text("Видео")
        .attachment(NewMessageAttachment.media(
                MessageAttachmentType.VIDEO,
                new AttachmentInput(null, token, null),
                "clip.mp4",
                result.contentTypeOptional().orElse(null),
                result.bytesTransferred()
        )));
```

## Отправить ссылку без получения token

Если MAX принимает attachment по URL, upload не нужен: URL кладется прямо в `AttachmentInput`.

```java
Message sent = messaging.send(chatId, Messages.text("Файл по ссылке")
        .attachment(NewMessageAttachment.media(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, null, "https://example.com/files/report.pdf"),
                "report.pdf",
                "application/pdf",
                null
        )));
```

## Публикация поста

```java
MessageBuilder post = Messages.markdown("""
        *Release 1.0*

        Status: published
        """)
        .attachment(NewMessageAttachment.imageUrl("https://example.com/release-cover.png"))
        .keyboard(Keyboards.inline(k -> k.row(
                Buttons.callback("Refresh", "post:refresh"),
                Buttons.link("Details", "https://example.com/release")
        )));

Message published = messaging.send(chatId, post);
MessageId postMessageId = published.messageId();
```

## Редактирование поста

```java
MessageBuilder editedPost = Messages.markdown("""
        *Release 1.0*

        Status: edited after publication
        """)
        .attachment(NewMessageAttachment.imageUrl("https://example.com/release-cover.png"))
        .attachment(NewMessageAttachment.shareUrl("https://example.com/release/1.0"))
        .keyboard(Keyboards.inline(k -> k.row(
                Buttons.callback("Refresh", "post:refresh"),
                Buttons.link("Details", "https://example.com/release")
        )));

boolean edited = messaging.edit(chatId, postMessageId, editedPost);
```

## Polling types для отслеживания публикации и редактирования

```java
PollingFetchRequest request = new PollingFetchRequest(
        null,
        30,
        100,
        List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_EDITED)
);
```

## Router для tracking публикации и редактирования

```java
Router router = new Router("post-tracking");
PostTracker postTracker = new PostTracker();

router.update(updateType(UpdateType.MESSAGE), update -> {
    Message message = update.message();
    if (message != null) {
        postTracker.onPublished(message);
        System.out.println("Published post/message: chat="
                + message.chat().id().value()
                + ", message_id=" + message.messageId().value()
                + ", raw_update_type=" + update.rawUpdateType()
                + ", channel=" + update.channel());
    }
    return CompletableFuture.completedFuture(null);
});

router.update(updateType(UpdateType.MESSAGE_EDITED), update -> {
    Message message = update.message();
    if (message != null) {
        postTracker.onEdited(message);
        System.out.println("Edited post/message: chat="
                + message.chat().id().value()
                + ", message_id=" + message.messageId().value()
                + ", raw_update_type=" + update.rawUpdateType());
    }
    return CompletableFuture.completedFuture(null);
});
```

Фильтр по типу update:

```java
private static Filter<Update> updateType(UpdateType expected) {
    return update -> CompletableFuture.completedFuture(
            update != null && update.type() == expected
                    ? FilterResult.matched()
                    : FilterResult.notMatched()
    );
}
```

Простейшее in-memory хранилище статусов:

```java
public static final class PostTracker {
    private final Map<MessageId, Message> published = new ConcurrentHashMap<>();
    private final Map<MessageId, Message> edited = new ConcurrentHashMap<>();

    public void onPublished(Message message) {
        if (message != null && message.messageId() != null) {
            published.put(message.messageId(), message);
        }
    }

    public void onEdited(Message message) {
        if (message != null && message.messageId() != null) {
            edited.put(message.messageId(), message);
            published.put(message.messageId(), message);
        }
    }

    public Map<MessageId, Message> published() {
        return Map.copyOf(published);
    }

    public Map<MessageId, Message> edited() {
        return Map.copyOf(edited);
    }
}
```

## Важные замечания

- Для media attachments используйте реальные `uploadToken` или `fileId`, полученные через upload flow/API.
- `PHOTO` при отправке мапится в API attachment type `image`, это учтено в `MaxApiModelMapper`.
- Для отслеживания через polling обязательно включите `MESSAGE_CREATED` и `MESSAGE_EDITED`.
- В normalized model событие `message_created` приходит как `UpdateType.MESSAGE`, а `message_edited` как `UpdateType.MESSAGE_EDITED`.
