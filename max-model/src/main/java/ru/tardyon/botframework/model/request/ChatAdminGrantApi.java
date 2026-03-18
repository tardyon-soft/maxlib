package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Docs-shaped admin grant entry for /members/admins.
 */
public record ChatAdminGrantApi(
        @JsonProperty("user_id") long userId,
        List<String> permissions,
        String alias
) {
    public ChatAdminGrantApi {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
        Objects.requireNonNull(permissions, "permissions");
    }
}
