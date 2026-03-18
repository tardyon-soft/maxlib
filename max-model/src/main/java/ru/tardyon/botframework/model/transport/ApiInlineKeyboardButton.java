package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docs-shaped inline keyboard button.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiInlineKeyboardButton(
        @JsonProperty("type") String type,
        @JsonProperty("text") String text,
        @JsonProperty("payload") String payload,
        @JsonProperty("url") String url,
        @JsonProperty("open_app") String openApp,
        @JsonProperty("message") String message,
        @JsonProperty("request_contact") Boolean requestContact,
        @JsonProperty("request_geo_location") Boolean requestGeoLocation
) {
}
