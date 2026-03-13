package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Webhook subscription DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Subscription(
        String url,
        @JsonProperty("update_types") List<UpdateEventType> updateTypes
) {
    public Subscription {
        Objects.requireNonNull(url, "url");
        updateTypes = updateTypes == null ? List.of() : List.copyOf(updateTypes);
    }
}
