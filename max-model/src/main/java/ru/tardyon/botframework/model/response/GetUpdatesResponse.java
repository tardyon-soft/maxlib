package ru.tardyon.botframework.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;
import ru.tardyon.botframework.model.Update;

/**
 * Typed getUpdates response envelope.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GetUpdatesResponse(
        List<Update> updates,
        Long marker
) {
    public GetUpdatesResponse {
        Objects.requireNonNull(updates, "updates");
        updates = List.copyOf(updates);
    }
}
