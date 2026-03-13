package ru.tardyon.botframework.client.pagination;

import java.util.Objects;

/**
 * Basic pagination input abstraction for marker-based MAX endpoints.
 *
 * @deprecated Use {@link MarkerPageRequest} for marker-based MAX APIs.
 */
@Deprecated(since = "0.1.0", forRemoval = false)
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

    public MarkerPageRequest toMarkerPageRequest() {
        return new MarkerPageRequest(limit, cursor);
    }
}
