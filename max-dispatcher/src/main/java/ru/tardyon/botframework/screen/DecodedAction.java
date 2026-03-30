package ru.tardyon.botframework.screen;

import java.util.Objects;

/**
 * Decoded logical action produced by {@link ScreenActionCodec}.
 *
 * @param <T> decoded payload shape
 */
public record DecodedAction<T>(
        String action,
        T payload
) {
    public DecodedAction {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(payload, "payload");
        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
    }
}

