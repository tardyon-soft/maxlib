package ru.max.botframework.ingestion;

import java.util.Objects;
import ru.max.botframework.model.Update;

/**
 * Framework-agnostic incoming webhook payload model.
 */
public record WebhookUpdatePayload(
        Update update,
        String secretHeader
) {
    public WebhookUpdatePayload {
        Objects.requireNonNull(update, "update");
    }
}
