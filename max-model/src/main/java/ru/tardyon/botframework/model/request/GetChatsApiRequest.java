package ru.tardyon.botframework.model.request;

/**
 * Docs-shaped request for GET /chats.
 */
public record GetChatsApiRequest(
        Long marker,
        Integer count
) {
    public GetChatsApiRequest {
        if (count != null && (count < 1 || count > 100)) {
            throw new IllegalArgumentException("count must be between 1 and 100");
        }
    }

    public static GetChatsApiRequest defaults() {
        return new GetChatsApiRequest(null, null);
    }
}
