package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Message DTO used in update payloads and API responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(
        MessageId messageId,
        Chat chat,
        User from,
        String text,
        Instant createdAt,
        MessageId replyToMessageId,
        List<MessageEntity> entities,
        List<MessageAttachment> attachments
) {
    public Message {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(chat, "chat");
        entities = entities == null ? List.of() : List.copyOf(entities);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
