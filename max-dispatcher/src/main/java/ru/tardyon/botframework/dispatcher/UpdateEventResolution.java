package ru.tardyon.botframework.dispatcher;

import java.util.Objects;

/**
 * Result of mapping one update into dispatcher-level event type.
 */
public record UpdateEventResolution(ResolvedUpdateEventType eventType) {
    public UpdateEventResolution {
        Objects.requireNonNull(eventType, "eventType");
    }

    public static UpdateEventResolution message() {
        return new UpdateEventResolution(ResolvedUpdateEventType.MESSAGE);
    }

    public static UpdateEventResolution callback() {
        return new UpdateEventResolution(ResolvedUpdateEventType.CALLBACK);
    }

    public static UpdateEventResolution unsupported() {
        return new UpdateEventResolution(ResolvedUpdateEventType.UNSUPPORTED);
    }
}

