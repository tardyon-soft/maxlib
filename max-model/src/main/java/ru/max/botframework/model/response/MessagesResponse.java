package ru.max.botframework.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;
import ru.max.botframework.model.Message;

/**
 * Envelope response containing a message list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessagesResponse(List<Message> messages) {
    public MessagesResponse {
        Objects.requireNonNull(messages, "messages");
        messages = List.copyOf(messages);
    }
}
