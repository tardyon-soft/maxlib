package ru.tardyon.botframework.screen;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * One stack frame in screen navigation state.
 */
public record ScreenStackEntry(
        String screenId,
        Map<String, Object> params,
        Instant pushedAt
) {
    public ScreenStackEntry {
        Objects.requireNonNull(screenId, "screenId");
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(pushedAt, "pushedAt");
        if (screenId.isBlank()) {
            throw new IllegalArgumentException("screenId must not be blank");
        }
        params = Map.copyOf(params);
    }
}
