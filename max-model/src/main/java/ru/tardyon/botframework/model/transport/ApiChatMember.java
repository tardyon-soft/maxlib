package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import ru.tardyon.botframework.model.ChatAdminPermission;

/**
 * MAX API transport chat member shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiChatMember(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("username") String username,
        @JsonProperty("is_bot") Boolean isBot,
        @JsonProperty("last_activity_time") Long lastActivityTime,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("full_avatar_url") String fullAvatarUrl,
        @JsonProperty("last_access_time") Long lastAccessTime,
        @JsonProperty("is_owner") Boolean isOwner,
        @JsonProperty("is_admin") Boolean isAdmin,
        @JsonProperty("join_time") Long joinTime,
        @JsonProperty("permissions") List<ChatAdminPermission> permissions,
        @JsonProperty("alias") String alias,
        @JsonProperty("chat_id") Long chatId,
        @JsonProperty("status") String status,
        @JsonProperty("user") ApiUser user
) {
    public ApiChatMember {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
