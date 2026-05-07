package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Rich-text markup segment inside message text.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageEntity(
        @JsonProperty("type") MessageEntityType type,
        @JsonProperty("from") @JsonAlias({"offset", "start"}) int offset,
        @JsonProperty("length") @JsonAlias("len") int length,
        @JsonProperty("value") String value,
        @JsonProperty("url") String url,
        @JsonProperty("user_link") @JsonAlias("userLink") String userLink,
        @JsonProperty("user_id") @JsonAlias("userId") Long userId
) {
    public MessageEntity {
        Objects.requireNonNull(type, "type");
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("offset and length must be non-negative");
        }
        if (value == null || value.isBlank()) {
            value = firstNonBlank(url, userLink, userId == null ? null : userId.toString());
        }
    }

    public MessageEntity(MessageEntityType type, int offset, int length, String value) {
        this(type, offset, length, value, null, null, null);
    }

    public MessageEntity(MessageEntityType type, int offset, int length, String url, String userLink, Long userId) {
        this(type, offset, length, null, url, userLink, userId);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
