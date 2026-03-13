package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import ru.tardyon.botframework.model.MessageAttachmentType;

/**
 * Minimal outgoing attachment DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NewMessageAttachment(
        MessageAttachmentType type,
        AttachmentInput input,
        InlineKeyboardAttachment inlineKeyboard,
        String caption,
        String mimeType,
        Long size
) {
    public NewMessageAttachment {
        Objects.requireNonNull(type, "type");
        if (input == null && inlineKeyboard == null) {
            throw new IllegalArgumentException("Either input or inlineKeyboard must be provided");
        }
        if (input != null && inlineKeyboard != null) {
            throw new IllegalArgumentException("input and inlineKeyboard are mutually exclusive");
        }
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }

    public static NewMessageAttachment media(
            MessageAttachmentType type,
            AttachmentInput input,
            String caption,
            String mimeType,
            Long size
    ) {
        return new NewMessageAttachment(type, input, null, caption, mimeType, size);
    }

    public static NewMessageAttachment inlineKeyboard(InlineKeyboardAttachment keyboard) {
        return new NewMessageAttachment(MessageAttachmentType.INLINE_KEYBOARD, null, keyboard, null, null, null);
    }
}
