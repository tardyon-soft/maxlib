package ru.tardyon.botframework.examples.attachments;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.dispatcher.Filter;
import ru.tardyon.botframework.dispatcher.FilterResult;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.MessageBuilder;
import ru.tardyon.botframework.message.MessageTarget;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.FileId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.request.AttachmentInput;
import ru.tardyon.botframework.model.request.NewMessageAttachment;

/**
 * Examples for:
 * - sending every supported outgoing attachment kind;
 * - sending a list of attachments in one message/post;
 * - publishing and editing a post;
 * - tracking publication and edit events through the update pipeline.
 */
public final class AttachmentAndPostExamples {
    private final MessagingFacade messaging;
    private final PostTracker postTracker = new PostTracker();

    public AttachmentAndPostExamples(MaxBotClient botClient) {
        this.messaging = new MessagingFacade(botClient);
    }

    public void sendEveryAttachmentKind(ChatId chatId) {
        MessageTarget target = MessageTarget.chat(chatId);

        messaging.send(target, Messages.text("IMAGE by URL")
                .attachment(NewMessageAttachment.imageUrl("https://example.com/image.png")));

        messaging.send(target, Messages.text("IMAGE by upload token")
                .attachment(NewMessageAttachment.imageToken("uploaded-image-token")));

        messaging.send(target, Messages.text("PHOTO by upload token")
                .attachment(media(MessageAttachmentType.PHOTO, uploadToken("uploaded-photo-token"), "Photo caption")));

        messaging.send(target, Messages.text("VIDEO by upload token")
                .attachment(media(MessageAttachmentType.VIDEO, uploadToken("uploaded-video-token"), "Video caption")));

        messaging.send(target, Messages.text("AUDIO by upload token")
                .attachment(media(MessageAttachmentType.AUDIO, uploadToken("uploaded-audio-token"), "Audio caption")));

        messaging.send(target, Messages.text("DOCUMENT by file id")
                .attachment(media(MessageAttachmentType.DOCUMENT, fileId("document-file-id"), "Document caption")));

        messaging.send(target, Messages.text("FILE by upload token")
                .attachment(media(MessageAttachmentType.FILE, uploadToken("uploaded-file-token"), "File caption")));

        messaging.send(target, Messages.text("STICKER")
                .attachment(NewMessageAttachment.sticker("sticker-code")));

        messaging.send(target, Messages.text("LOCATION")
                .attachment(NewMessageAttachment.location(55.751244, 37.618423)));

        messaging.send(target, Messages.text("SHARE by URL")
                .attachment(NewMessageAttachment.shareUrl("https://example.com/post/123")));

        messaging.send(target, Messages.text("SHARE by token")
                .attachment(NewMessageAttachment.shareToken("shared-attachment-token")));

        messaging.send(target, Messages.text("INLINE_KEYBOARD")
                .keyboard(Keyboards.inline(k -> k.row(
                        Buttons.callback("Edit post", "post:edit"),
                        Buttons.link("Open", "https://example.com")
                ))));
    }

    public Message sendAttachmentListPost(ChatId channelOrChatId) {
        List<NewMessageAttachment> attachments = List.of(
                NewMessageAttachment.imageUrl("https://example.com/cover.png"),
                media(MessageAttachmentType.VIDEO, uploadToken("uploaded-video-token"), "Demo video"),
                media(MessageAttachmentType.FILE, fileId("release-notes-file-id"), "Release notes"),
                NewMessageAttachment.location(55.751244, 37.618423),
                NewMessageAttachment.shareUrl("https://example.com/release")
        );

        return messaging.send(channelOrChatId, Messages.markdown("""
                *Release post*

                Attachments are sent as one list in the message body.
                """).attachments(attachments));
    }

    public Message publishAndEditPost(ChatId channelOrChatId) {
        Message published = messaging.send(channelOrChatId, releasePost("Release 1.0", "Status: published"));
        postTracker.onPublished(published);

        boolean edited = messaging.edit(
                channelOrChatId,
                published.messageId(),
                releasePost("Release 1.0", "Status: edited after publication")
                        .attachment(NewMessageAttachment.shareUrl("https://example.com/release/1.0"))
        );

        if (!edited) {
            throw new IllegalStateException("Post edit was not accepted by MAX API");
        }
        return published;
    }

    public Router postTrackingRouter() {
        Router router = new Router("post-tracking");

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

        return router;
    }

    public PollingFetchRequest pollingRequestForPublicationAndEditTracking() {
        return new PollingFetchRequest(
                null,
                30,
                100,
                List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_EDITED)
        );
    }

    private static MessageBuilder releasePost(String title, String status) {
        return Messages.markdown("*" + title + "*\n\n" + status)
                .attachment(NewMessageAttachment.imageUrl("https://example.com/release-cover.png"))
                .keyboard(Keyboards.inline(k -> k.row(
                        Buttons.callback("Refresh", "post:refresh"),
                        Buttons.link("Details", "https://example.com/release")
                )));
    }

    private static NewMessageAttachment media(
            MessageAttachmentType type,
            AttachmentInput input,
            String caption
    ) {
        return NewMessageAttachment.media(type, input, caption, null, null);
    }

    private static AttachmentInput uploadToken(String token) {
        return new AttachmentInput(null, token, null);
    }

    private static AttachmentInput fileId(String value) {
        return new AttachmentInput(new FileId(value), null, null);
    }

    private static Filter<Update> updateType(UpdateType expected) {
        return update -> CompletableFuture.completedFuture(
                update != null && update.type() == expected
                        ? FilterResult.matched()
                        : FilterResult.notMatched()
        );
    }

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
}
