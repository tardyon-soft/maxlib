package ru.max.botframework.client.pagination;

import java.util.Objects;

/**
 * Basic pagination input abstraction for cursor-based MAX endpoints.
 */
public record PageRequest(int limit, String cursor) {

    public PageRequest {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Objects.requireNonNull(cursor, "cursor");
    }

    public static PageRequest firstPage(int limit) {
        return new PageRequest(limit, "");
    }
}
