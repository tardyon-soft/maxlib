package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport user shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiUser(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("username") String username,
        @JsonProperty("is_bot") Boolean isBot,
        @JsonProperty("last_activity_time") Long lastActivityTime,
        @JsonProperty("name") String name
) {
}
