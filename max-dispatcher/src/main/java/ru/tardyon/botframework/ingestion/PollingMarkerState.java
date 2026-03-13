package ru.tardyon.botframework.ingestion;

/**
 * Mutable marker holder for long polling progression.
 *
 * <p>Implementations may later be backed by persistent storage.</p>
 */
public interface PollingMarkerState {

    Long current();

    void advance(Long marker);
}
