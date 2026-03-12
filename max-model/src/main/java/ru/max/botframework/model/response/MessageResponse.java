package ru.max.botframework.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import ru.max.botframework.model.Message;

/**
 * Envelope response containing a single message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageResponse(Message message) {
    public MessageResponse {
        Objects.requireNonNull(message, "message");
    }
}
