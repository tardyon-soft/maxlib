package ru.tardyon.botframework.model.request;

/**
 * Docs-shaped request for GET /messages pagination/filtering.
 */
public record GetMessagesApiRequest(
        Long chatId,
        Long from,
        Long to,
        Integer count
) {
    public GetMessagesApiRequest {
        if (count != null && (count < 1 || count > 100)) {
            throw new IllegalArgumentException("count must be between 1 and 100");
        }
        if (from != null && to != null && from > to) {
            throw new IllegalArgumentException("from must be less than or equal to to");
        }
    }
}
