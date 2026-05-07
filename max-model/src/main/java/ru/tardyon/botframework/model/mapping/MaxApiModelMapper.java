package ru.tardyon.botframework.model.mapping;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatMember;
import ru.tardyon.botframework.model.ChatMemberStatus;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageEntity;
import ru.tardyon.botframework.model.MessageEntityType;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.FileId;
import ru.tardyon.botframework.model.MessageAttachment;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.request.NewMessageAttachment;
import ru.tardyon.botframework.model.request.AttachmentInput;
import ru.tardyon.botframework.model.request.InlineKeyboardButtonRequest;
import ru.tardyon.botframework.model.transport.ApiCallback;
import ru.tardyon.botframework.model.transport.ApiAttachmentRequest;
import ru.tardyon.botframework.model.transport.ApiChat;
import ru.tardyon.botframework.model.transport.ApiChatMember;
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

    public static ChatMember toNormalized(ApiChatMember source) {
        Objects.requireNonNull(source, "source");
        Chat chat = new Chat(
                new ChatId(stringId(source.chatId(), "chat")),
                ChatType.UNKNOWN,
                null,
                null,
                null
        );
        User user = source.user() == null
                ? new User(new UserId(stringId(source.userId(), "user")), null, null, null, null, null, null)
                : toNormalized(source.user());
        return new ChatMember(
                chat,
                user,
                ChatMemberStatus.fromValue(source.status()),
                null,
                null
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
                source.body() == null ? java.util.List.of() : mapIncomingMarkup(source.body().markup()),
                source.body() == null ? java.util.List.of() : mapIncomingAttachments(source.body().attachments())
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
        ChatMember chatMember = source.chatMember() == null ? null : toNormalized(source.chatMember());
        ChatId chatId = source.chatId() == null ? null : new ChatId(source.chatId().toString());
        User user = source.user() == null ? null : toNormalized(source.user());
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
                chatMember,
                chatId,
                user,
                source.isChannel(),
                toInstant(source.timestamp()),
                source.updateType(),
                source.payload(),
                source.title(),
                source.userLocale()
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
        if (attachment.input() != null) {
            return new ApiAttachmentRequest(outgoingAttachmentType(attachment.type()), mapAttachmentInput(attachment.input()));
        }
        if (attachment.payload() != null) {
            return new ApiAttachmentRequest(outgoingAttachmentType(attachment.type()), attachment.payload());
        }
        return attachment;
    }

    private static String outgoingAttachmentType(MessageAttachmentType type) {
        if (type == MessageAttachmentType.PHOTO || type == MessageAttachmentType.IMAGE) {
            return MessageAttachmentType.IMAGE.value();
        }
        return type.value();
    }

    private static Map<String, Object> mapAttachmentInput(AttachmentInput input) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (input.url() != null && !input.url().isBlank()) {
            payload.put("url", input.url());
        }
        if (input.uploadRef() != null && !input.uploadRef().isBlank()) {
            payload.put("token", input.uploadRef());
        }
        if (input.fileId() != null) {
            payload.put("file_id", input.fileId().value());
        }
        return Map.copyOf(payload);
    }

    private static List<MessageAttachment> mapIncomingAttachments(List<Object> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(MaxApiModelMapper::mapIncomingAttachment)
                .filter(Objects::nonNull)
                .toList();
    }

    private static MessageAttachment mapIncomingAttachment(Object attachment) {
        if (attachment instanceof MessageAttachment messageAttachment) {
            return messageAttachment;
        }
        if (!(attachment instanceof Map<?, ?> map)) {
            return null;
        }

        MessageAttachmentType type = MessageAttachmentType.fromValue(stringValue(map.get("type")));
        Object payload = map.get("payload");
        String fileId = firstNonBlank(
                stringValue(map.get("fileId")),
                stringValue(map.get("file_id")),
                payloadValue(payload, "fileId"),
                payloadValue(payload, "file_id")
        );
        String url = firstNonBlank(stringValue(map.get("url")), payloadValue(payload, "url"));
        String mimeType = firstNonBlank(
                stringValue(map.get("mimeType")),
                stringValue(map.get("mime_type")),
                payloadValue(payload, "mimeType"),
                payloadValue(payload, "mime_type")
        );
        Long size = longValue(map.get("size"));
        if (size == null) {
            size = longValue(payloadMapValue(payload, "size"));
        }
        return new MessageAttachment(
                type,
                fileId == null ? null : new FileId(fileId),
                url,
                mimeType,
                size,
                payload
        );
    }

    private static List<MessageEntity> mapIncomingMarkup(List<Object> markup) {
        if (markup == null || markup.isEmpty()) {
            return List.of();
        }
        return markup.stream()
                .map(MaxApiModelMapper::mapIncomingEntity)
                .filter(Objects::nonNull)
                .toList();
    }

    private static MessageEntity mapIncomingEntity(Object entity) {
        if (entity instanceof MessageEntity messageEntity) {
            return messageEntity;
        }
        if (!(entity instanceof Map<?, ?> map)) {
            return null;
        }

        Object payload = map.get("payload");
        MessageEntityType type = MessageEntityType.fromValue(firstNonBlank(
                stringValue(map.get("type")),
                payloadValue(payload, "type")
        ));
        Integer offset = intValue(firstNonNull(
                map.get("from"),
                map.get("offset"),
                map.get("start"),
                payloadMapValue(payload, "from"),
                payloadMapValue(payload, "offset"),
                payloadMapValue(payload, "start")
        ));
        Integer length = intValue(firstNonNull(
                map.get("length"),
                map.get("len"),
                payloadMapValue(payload, "length"),
                payloadMapValue(payload, "len")
        ));
        if (offset == null || length == null) {
            return null;
        }
        String url = firstNonBlank(
                stringValue(map.get("url")),
                stringValue(map.get("link")),
                payloadValue(payload, "url"),
                payloadValue(payload, "link")
        );
        String userLink = firstNonBlank(
                stringValue(map.get("user_link")),
                stringValue(map.get("userLink")),
                payloadValue(payload, "user_link"),
                payloadValue(payload, "userLink")
        );
        Long userId = longValue(firstNonNull(
                map.get("user_id"),
                map.get("userId"),
                payloadMapValue(payload, "user_id"),
                payloadMapValue(payload, "userId")
        ));
        return new MessageEntity(type, offset, length, url, userLink, userId);
    }

    private static String payloadValue(Object payload, String key) {
        return stringValue(payloadMapValue(payload, key));
    }

    private static Object payloadMapValue(Object payload, String key) {
        if (payload instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Integer intValue(Object value) {
        Long longValue = longValue(value);
        if (longValue == null || longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
            return null;
        }
        return longValue.intValue();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
            case "message_edited" -> UpdateType.MESSAGE_EDITED;
            case "message_removed" -> UpdateType.MESSAGE_REMOVED;
            case "bot_added" -> UpdateType.BOT_ADDED;
            case "bot_removed" -> UpdateType.BOT_REMOVED;
            case "bot_started" -> UpdateType.BOT_STARTED;
            case "bot_stopped" -> UpdateType.BOT_STOPPED;
            case "user_added" -> UpdateType.USER_ADDED;
            case "user_removed" -> UpdateType.USER_REMOVED;
            case "chat_title_changed" -> UpdateType.CHAT_TITLE_CHANGED;
            case "message_chat_created" -> UpdateType.MESSAGE_CHAT_CREATED;
            case "message_construction_request" -> UpdateType.MESSAGE_CONSTRUCTION_REQUEST;
            case "message_constructed" -> UpdateType.MESSAGE_CONSTRUCTED;
            case "dialog_muted" -> UpdateType.DIALOG_MUTED;
            case "dialog_unmuted" -> UpdateType.DIALOG_UNMUTED;
            case "dialog_cleared" -> UpdateType.DIALOG_CLEARED;
            case "dialog_removed" -> UpdateType.DIALOG_REMOVED;
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
        if (source.chatId() != null && source.updateType() != null && !source.updateType().isBlank()) {
            long ts = source.timestamp() == null ? 0L : source.timestamp();
            return "upd-" + source.updateType() + "-" + source.chatId() + "-" + ts;
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
