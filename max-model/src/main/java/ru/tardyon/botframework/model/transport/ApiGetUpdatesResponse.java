package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * MAX API transport response envelope for getUpdates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiGetUpdatesResponse(
        @JsonProperty("updates") List<ApiUpdate> updates,
        @JsonProperty("marker") Long marker
) {
    public ApiGetUpdatesResponse {
        updates = updates == null ? List.of() : List.copyOf(updates);
    }
}
