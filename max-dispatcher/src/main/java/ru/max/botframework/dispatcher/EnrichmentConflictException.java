package ru.max.botframework.dispatcher;

/**
 * Signals conflicting enrichment writes for the same runtime key.
 */
public final class EnrichmentConflictException extends IllegalStateException {

    private EnrichmentConflictException(String message) {
        super(message);
    }

    static EnrichmentConflictException conflict(String key, Object existing, Object incoming, String source) {
        String existingType = existing.getClass().getSimpleName();
        String incomingType = incoming.getClass().getSimpleName();
        return new EnrichmentConflictException(
                "enrichment conflict on key '%s' from %s: existing=%s (%s), incoming=%s (%s)"
                        .formatted(key, source, existing, existingType, incoming, incomingType)
        );
    }
}
