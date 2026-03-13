package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import ru.tardyon.botframework.model.UpdateEventType;

/**
 * Typed request for creating webhook subscription.
 */
public record CreateSubscriptionRequest(
        String url,
        @JsonProperty("update_types") List<UpdateEventType> updateTypes,
        String secret
) {
    public CreateSubscriptionRequest {
        Objects.requireNonNull(url, "url");
        if (url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        updateTypes = updateTypes == null ? List.of() : List.copyOf(updateTypes);
    }
}
