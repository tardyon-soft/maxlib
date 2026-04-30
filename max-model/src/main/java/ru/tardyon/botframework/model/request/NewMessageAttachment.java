package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import ru.tardyon.botframework.model.MessageAttachmentType;

/**
 * Minimal outgoing attachment DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NewMessageAttachment(
        MessageAttachmentType type,
        AttachmentInput input,
        InlineKeyboardAttachment inlineKeyboard,
        Object payload,
        String caption,
        String mimeType,
        Long size
) {
    public NewMessageAttachment {
        Objects.requireNonNull(type, "type");
        int payloadSources = (input == null ? 0 : 1)
                + (inlineKeyboard == null ? 0 : 1)
                + (payload == null ? 0 : 1);
        if (payloadSources == 0) {
            throw new IllegalArgumentException("Either input, inlineKeyboard or payload must be provided");
        }
        if (payloadSources > 1) {
            throw new IllegalArgumentException("input, inlineKeyboard and payload are mutually exclusive");
        }
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }

    public NewMessageAttachment(
            MessageAttachmentType type,
            AttachmentInput input,
            InlineKeyboardAttachment inlineKeyboard,
            String caption,
            String mimeType,
            Long size
    ) {
        this(type, input, inlineKeyboard, null, caption, mimeType, size);
    }

    public static NewMessageAttachment media(
            MessageAttachmentType type,
            AttachmentInput input,
            String caption,
            String mimeType,
            Long size
    ) {
        return new NewMessageAttachment(type, input, null, null, caption, mimeType, size);
    }

    public static NewMessageAttachment inlineKeyboard(InlineKeyboardAttachment keyboard) {
        return new NewMessageAttachment(MessageAttachmentType.INLINE_KEYBOARD, null, keyboard, null, null, null, null);
    }

    public static NewMessageAttachment payload(MessageAttachmentType type, Object payload) {
        return new NewMessageAttachment(type, null, null, Objects.requireNonNull(payload, "payload"), null, null, null);
    }

    public static NewMessageAttachment imageUrl(String url) {
        return media(MessageAttachmentType.IMAGE, new AttachmentInput(null, null, url), null, null, null);
    }

    public static NewMessageAttachment imageToken(String token) {
        return media(MessageAttachmentType.IMAGE, new AttachmentInput(null, token, null), null, null, null);
    }

    public static NewMessageAttachment sticker(String code) {
        return payload(MessageAttachmentType.STICKER, new StickerAttachmentRequestPayload(code));
    }

    public static NewMessageAttachment location(double latitude, double longitude) {
        return payload(MessageAttachmentType.LOCATION, new LocationAttachmentRequestPayload(latitude, longitude));
    }

    public static NewMessageAttachment shareUrl(String url) {
        return payload(MessageAttachmentType.SHARE, ShareAttachmentRequestPayload.url(url));
    }

    public static NewMessageAttachment shareToken(String token) {
        return payload(MessageAttachmentType.SHARE, ShareAttachmentRequestPayload.token(token));
    }

    public static NewMessageAttachment share(String url, String token) {
        return payload(MessageAttachmentType.SHARE, new ShareAttachmentRequestPayload(url, token));
    }
}
