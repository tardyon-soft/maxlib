package ru.tardyon.botframework.model.mapping;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.request.NewMessageAttachment;
import ru.tardyon.botframework.model.request.InlineKeyboardButtonRequest;
import ru.tardyon.botframework.model.transport.ApiCallback;
import ru.tardyon.botframework.model.transport.ApiAttachmentRequest;
import ru.tardyon.botframework.model.transport.ApiChat;
import ru.tardyon.botframework.model.transport.ApiInlineKeyboardButton;
import ru.tardyon.botframework.model.transport.ApiInlineKeyboardPayload;
import ru.tardyon.botframework.model.transport.ApiMessage;
import ru.tardyon.botframework.model.transport.ApiNewMessageBody;
import ru.tardyon.botframework.model.transport.ApiNewMessageLink;
import ru.tardyon.botframework.model.transport.ApiOutgoingMessageBody;
import ru.tardyon.botframework.model.transport.ApiUpdate;
import ru.tardyon.botframework.model.transport.ApiUser;

/**
 * Mapping between MAX API transport DTO and normalized runtime DTO.
 */
public final class MaxApiModelMapper {

    private MaxApiModelMapper() {
    }

    public static User toNormalized(ApiUser source) {
        Objects.requireNonNull(source, "source");
        return new User(
                new UserId(stringId(source.userId(), "user")),
                source.username(),
                source.firstName(),
                source.lastName(),
                displayName(source),
                source.isBot(),
                null
        );
    }

    public static Chat toNormalized(ApiChat source) {
        Objects.requireNonNull(source, "source");
        return new Chat(
                new ChatId(stringId(source.chatId(), "chat")),
                chatType(source.type()),
                source.title(),
                null,
                source.description()
        );
    }

    public static Message toNormalized(ApiMessage source) {
        Objects.requireNonNull(source, "source");
        Chat chat = toNormalizedChat(source);
        MessageId replyTo = null;
        if (source.link() != null && source.link().message() != null) {
            String linkedMessageId = source.link().message().mid();
            if (linkedMessageId != null && !linkedMessageId.isBlank()) {
                replyTo = new MessageId(linkedMessageId);
            }
        }

        return new Message(
                new MessageId(stringId(extractMessageId(source), "msg")),
                chat,
                source.sender() == null ? null : toNormalized(source.sender()),
                source.body() == null ? null : source.body().text(),
                toInstant(source.timestamp()),
                replyTo,
                java.util.List.of(),
                java.util.List.of()
        );
    }

    public static Callback toNormalized(ApiCallback source) {
        Objects.requireNonNull(source, "source");
        Message callbackMessage = source.message() == null ? fallbackCallbackMessage(source) : toNormalized(source.message());
        return new Callback(
                new CallbackId(stringId(source.callbackId(), "cb")),
                source.payload(),
                source.sender() == null ? null : toNormalized(source.sender()),
                callbackMessage,
                toInstant(source.timestamp())
        );
    }

    public static Update toNormalized(ApiUpdate source) {
        Objects.requireNonNull(source, "source");
        UpdateType type = updateType(source.updateType());
        Message message = source.message() == null ? null : toNormalized(source.message());
        Callback callback = source.callback() == null ? null : toNormalized(source.callback());
        if (callback != null && callback.message() == null && message != null) {
            callback = new Callback(
                    callback.callbackId(),
                    callback.data(),
                    callback.from(),
                    message,
                    callback.createdAt()
            );
        }

        return new Update(
                new UpdateId(syntheticUpdateId(source)),
                type,
                message,
                callback,
                null,
                toInstant(source.timestamp())
        );
    }

    public static NewMessageBody toNormalized(ApiNewMessageBody source) {
        Objects.requireNonNull(source, "source");
        TextFormat format = source.format() == null ? TextFormat.PLAIN : source.format();
        return new NewMessageBody(source.text(), format, source.attachments());
    }

    public static ApiNewMessageBody toApi(NewMessageBody source) {
        return toApi(source, Boolean.TRUE, null);
    }

    public static ApiNewMessageBody toApi(NewMessageBody source, Boolean notify, MessageId replyToMessageId) {
        Objects.requireNonNull(source, "source");
        return new ApiNewMessageBody(
                source.text(),
                source.attachments(),
                replyToMessageId == null ? null : new ApiNewMessageLink("reply", replyToMessageId.value()),
                notify,
                source.format() == TextFormat.PLAIN ? null : source.format()
        );
    }

    public static ApiOutgoingMessageBody toApiOutgoing(NewMessageBody source, Boolean notify, MessageId replyToMessageId) {
        Objects.requireNonNull(source, "source");
        return new ApiOutgoingMessageBody(
                source.text(),
                mapOutgoingAttachments(source.attachments()),
                replyToMessageId == null ? null : new ApiNewMessageLink("reply", replyToMessageId.value()),
                notify,
                source.format() == TextFormat.PLAIN ? null : source.format()
        );
    }

    private static List<Object> mapOutgoingAttachments(List<NewMessageAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream().map(MaxApiModelMapper::mapOutgoingAttachment).toList();
    }

    private static Object mapOutgoingAttachment(NewMessageAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        if (attachment.type() == MessageAttachmentType.INLINE_KEYBOARD && attachment.inlineKeyboard() != null) {
            List<List<ApiInlineKeyboardButton>> buttons = attachment.inlineKeyboard().rows().stream()
                    .map(row -> row.stream().map(MaxApiModelMapper::mapInlineButton).toList())
                    .toList();
            return new ApiAttachmentRequest(MessageAttachmentType.INLINE_KEYBOARD.value(), new ApiInlineKeyboardPayload(buttons));
        }
        return attachment;
    }

    private static ApiInlineKeyboardButton mapInlineButton(InlineKeyboardButtonRequest button) {
        String type;
        String payload = null;
        if (button.clipboardPayload() != null && !button.clipboardPayload().isBlank()) {
            type = "clipboard";
            payload = button.clipboardPayload();
        } else if (button.callbackData() != null && !button.callbackData().isBlank()) {
            type = "callback";
            payload = button.callbackData();
        } else if (button.url() != null && !button.url().isBlank()) {
            type = "link";
        } else if (Boolean.TRUE.equals(button.requestContact())) {
            type = "request_contact";
        } else if (Boolean.TRUE.equals(button.requestGeoLocation())) {
            type = "request_geo_location";
        } else if (button.openApp() != null && !button.openApp().isBlank()) {
            type = "open_app";
        } else {
            type = "message";
        }
        return new ApiInlineKeyboardButton(
                type,
                button.text(),
                payload,
                button.url(),
                button.openApp(),
                button.message(),
                button.requestContact(),
                button.requestGeoLocation()
        );
    }

    private static Chat toNormalizedChat(ApiMessage source) {
        if (source.recipient() != null && source.recipient().chatId() != null) {
            return new Chat(
                    new ChatId(source.recipient().chatId().toString()),
                    chatType(source.recipient().chatType()),
                    null,
                    null,
                    null
            );
        }
        if (source.sender() != null && source.sender().userId() != null) {
            return new Chat(
                    new ChatId("dm-" + source.sender().userId()),
                    ChatType.PRIVATE,
                    null,
                    null,
                    null
            );
        }
        return new Chat(new ChatId("chat-unknown"), ChatType.UNKNOWN, null, null, null);
    }

    private static Message fallbackCallbackMessage(ApiCallback source) {
        if (source.messageId() == null || source.messageId().isBlank()) {
            return null;
        }

        Chat chat = source.chatId() == null || source.chatId().isBlank()
                ? new Chat(new ChatId("chat-unknown"), ChatType.UNKNOWN, null, null, null)
                : new Chat(new ChatId(source.chatId()), ChatType.UNKNOWN, null, null, null);

        return new Message(
                new MessageId(source.messageId()),
                chat,
                source.sender() == null ? null : toNormalized(source.sender()),
                null,
                toInstant(source.timestamp()),
                null,
                List.of(),
                List.of()
        );
    }

    private static UpdateType updateType(String value) {
        if (value == null) {
            return UpdateType.UNKNOWN;
        }
        return switch (value) {
            case "message_created" -> UpdateType.MESSAGE;
            case "message_callback" -> UpdateType.CALLBACK;
            case "chat_member" -> UpdateType.CHAT_MEMBER;
            default -> UpdateType.UNKNOWN;
        };
    }

    private static ChatType chatType(String value) {
        if (value == null || value.isBlank()) {
            return ChatType.UNKNOWN;
        }
        return switch (value) {
            case "chat" -> ChatType.GROUP;
            case "dialog" -> ChatType.PRIVATE;
            default -> ChatType.fromValue(value);
        };
    }

    private static Instant toInstant(Long unixTime) {
        if (unixTime == null) {
            return null;
        }
        if (unixTime >= 1_000_000_000_000L) {
            return Instant.ofEpochMilli(unixTime);
        }
        return Instant.ofEpochSecond(unixTime);
    }

    private static String syntheticUpdateId(ApiUpdate source) {
        if (source.callback() != null && source.callback().callbackId() != null && !source.callback().callbackId().isBlank()) {
            return "upd-cb-" + source.callback().callbackId();
        }
        String messageId = extractMessageId(source.message());
        if (messageId != null && !messageId.isBlank()) {
            return "upd-msg-" + messageId;
        }
        String type = source.updateType();
        Long timestamp = source.timestamp();
        String normalizedType = type == null || type.isBlank() ? "unknown" : type;
        long ts = timestamp == null ? 0L : timestamp;
        return "upd-" + normalizedType + "-" + ts;
    }

    private static String stringId(Long value, String prefix) {
        if (value != null) {
            return value.toString();
        }
        return prefix + "-unknown";
    }

    private static String stringId(String value, String prefix) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return prefix + "-unknown";
    }

    private static String extractMessageId(ApiMessage source) {
        if (source == null) {
            return null;
        }
        if (source.body() != null && source.body().mid() != null && !source.body().mid().isBlank()) {
            return source.body().mid();
        }
        return source.messageId();
    }

    private static String displayName(ApiUser user) {
        if (user.name() != null && !user.name().isBlank()) {
            return user.name();
        }
        String first = user.firstName() == null ? "" : user.firstName().trim();
        String last = user.lastName() == null ? "" : user.lastName().trim();
        String joined = (first + " " + last).trim();
        return joined.isEmpty() ? null : joined;
    }
}
