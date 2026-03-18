package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Docs-shaped payload for inline keyboard attachment.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiInlineKeyboardPayload(
        @JsonProperty("buttons") List<List<ApiInlineKeyboardButton>> buttons
) {
    public ApiInlineKeyboardPayload {
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
    }
}
