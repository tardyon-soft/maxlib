package ru.max.botframework.model.request;

import java.util.List;
import ru.max.botframework.model.UpdateEventType;

/**
 * Typed polling request options for getUpdates API.
 */
public record GetUpdatesRequest(
        Long marker,
        Integer timeout,
        Integer limit,
        List<UpdateEventType> types
) {
    public GetUpdatesRequest {
        types = types == null ? List.of() : List.copyOf(types);

        if (timeout != null && (timeout < 0 || timeout > 90)) {
            throw new IllegalArgumentException("timeout must be between 0 and 90 seconds");
        }
        if (limit != null && (limit < 1 || limit > 1000)) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
    }

    public static GetUpdatesRequest defaults() {
        return new GetUpdatesRequest(null, null, null, List.of());
    }
}
