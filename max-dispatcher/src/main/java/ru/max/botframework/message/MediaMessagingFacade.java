package ru.max.botframework.message;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.UserId;
import ru.max.botframework.upload.InputFile;
import ru.max.botframework.upload.UploadMediaKind;
import ru.max.botframework.upload.UploadRequest;
import ru.max.botframework.upload.UploadResult;
import ru.max.botframework.upload.UploadService;

/**
 * High-level media send/reply facade over {@link UploadService} and existing {@link MessagingFacade}.
 */
public final class MediaMessagingFacade {
    private final UploadService uploadService;
    private final MessagingFacade messaging;

    public MediaMessagingFacade(UploadService uploadService, MessagingFacade messaging) {
        this.uploadService = Objects.requireNonNull(uploadService, "uploadService");
        this.messaging = Objects.requireNonNull(messaging, "messaging");
    }

    public Message sendImage(MessageTarget target, InputFile inputFile) {
        return sendImage(target, inputFile, null);
    }

    public Message sendImage(MessageTarget target, InputFile inputFile, String caption) {
        return send(target, inputFile, UploadMediaKind.IMAGE, caption);
    }

    public Message sendImage(ChatId chatId, InputFile inputFile) {
        return sendImage(MessageTarget.chat(Objects.requireNonNull(chatId, "chatId")), inputFile);
    }

    public Message sendImage(UserId userId, InputFile inputFile) {
        return sendImage(MessageTarget.user(Objects.requireNonNull(userId, "userId")), inputFile);
    }

    public Message sendFile(MessageTarget target, InputFile inputFile) {
        return sendFile(target, inputFile, null);
    }

    public Message sendFile(MessageTarget target, InputFile inputFile, String caption) {
        return send(target, inputFile, UploadMediaKind.FILE, caption);
    }

    public Message sendVideo(MessageTarget target, InputFile inputFile) {
        return sendVideo(target, inputFile, null);
    }

    public Message sendVideo(MessageTarget target, InputFile inputFile, String caption) {
        return send(target, inputFile, UploadMediaKind.VIDEO, caption);
    }

    public Message sendAudio(MessageTarget target, InputFile inputFile) {
        return sendAudio(target, inputFile, null);
    }

    public Message sendAudio(MessageTarget target, InputFile inputFile, String caption) {
        return send(target, inputFile, UploadMediaKind.AUDIO, caption);
    }

    public Message replyImage(Message sourceMessage, InputFile inputFile) {
        return replyImage(sourceMessage, inputFile, null);
    }

    public Message replyImage(Message sourceMessage, InputFile inputFile, String caption) {
        return reply(sourceMessage, inputFile, UploadMediaKind.IMAGE, caption);
    }

    public Message replyFile(Message sourceMessage, InputFile inputFile) {
        return replyFile(sourceMessage, inputFile, null);
    }

    public Message replyFile(Message sourceMessage, InputFile inputFile, String caption) {
        return reply(sourceMessage, inputFile, UploadMediaKind.FILE, caption);
    }

    public Message replyVideo(Message sourceMessage, InputFile inputFile) {
        return replyVideo(sourceMessage, inputFile, null);
    }

    public Message replyVideo(Message sourceMessage, InputFile inputFile, String caption) {
        return reply(sourceMessage, inputFile, UploadMediaKind.VIDEO, caption);
    }

    public Message replyAudio(Message sourceMessage, InputFile inputFile) {
        return replyAudio(sourceMessage, inputFile, null);
    }

    public Message replyAudio(Message sourceMessage, InputFile inputFile, String caption) {
        return reply(sourceMessage, inputFile, UploadMediaKind.AUDIO, caption);
    }

    private Message send(MessageTarget target, InputFile inputFile, UploadMediaKind kind, String caption) {
        Objects.requireNonNull(target, "target");
        MediaAttachment attachment = buildAttachment(inputFile, kind, caption);
        return messaging.send(target, Messages.message().attachment(attachment));
    }

    private Message reply(Message sourceMessage, InputFile inputFile, UploadMediaKind kind, String caption) {
        Objects.requireNonNull(sourceMessage, "sourceMessage");
        MediaAttachment attachment = buildAttachment(inputFile, kind, caption);
        return messaging.reply(sourceMessage, Messages.message().attachment(attachment));
    }

    private MediaAttachment buildAttachment(InputFile inputFile, UploadMediaKind kind, String caption) {
        Objects.requireNonNull(inputFile, "inputFile");
        Objects.requireNonNull(kind, "kind");

        UploadRequest request = UploadRequest.defaults().withMediaTypeHint(kind.name().toLowerCase(Locale.ROOT));
        UploadResult uploadResult = await(uploadService.upload(inputFile, request));

        return switch (kind) {
            case IMAGE -> ImageAttachment.from(uploadResult).caption(caption);
            case FILE -> FileAttachment.from(uploadResult).caption(caption);
            case VIDEO -> VideoAttachment.from(uploadResult).caption(caption);
            case AUDIO -> AudioAttachment.from(uploadResult).caption(caption);
            case UNKNOWN -> throw new IllegalArgumentException("UNKNOWN media kind is not supported by media facade");
        };
    }

    private static <T> T await(CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().join();
        } catch (CompletionException completionException) {
            if (completionException.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw completionException;
        }
    }
}
