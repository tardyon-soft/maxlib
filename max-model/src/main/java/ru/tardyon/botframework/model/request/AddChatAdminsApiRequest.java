package ru.tardyon.botframework.model.request;

import java.util.List;
import java.util.Objects;

/**
 * Docs-shaped request for POST /chats/{chatId}/members/admins.
 */
public record AddChatAdminsApiRequest(
        List<ChatAdminGrantApi> admins
) {
    public AddChatAdminsApiRequest {
        Objects.requireNonNull(admins, "admins");
        if (admins.isEmpty()) {
            throw new IllegalArgumentException("admins must not be empty");
        }
        admins = List.copyOf(admins);
    }
}
