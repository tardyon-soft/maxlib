package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * MAX API transport response for GET /chats.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiChatsResponse(
        @JsonProperty("chats") List<ApiChat> chats,
        @JsonProperty("marker") Long marker
) {
    public ApiChatsResponse {
        chats = chats == null ? List.of() : List.copyOf(chats);
    }
}
