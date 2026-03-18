package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport chat shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiChat(
        @JsonProperty("chat_id") Long chatId,
        @JsonProperty("type") String type,
        @JsonProperty("status") String status,
        @JsonProperty("title") String title,
        @JsonProperty("icon") Object icon,
        @JsonProperty("last_event_time") Long lastEventTime,
        @JsonProperty("participants_count") Integer participantsCount,
        @JsonProperty("owner_id") Long ownerId,
        @JsonProperty("participants") Object participants,
        @JsonProperty("is_public") Boolean isPublic,
        @JsonProperty("link") String link,
        @JsonProperty("description") String description,
        @JsonProperty("dialog_with_user") ApiUser dialogWithUser,
        @JsonProperty("chat_message_id") String chatMessageId,
        @JsonProperty("pinned_message") ApiMessage pinnedMessage
) {
}
