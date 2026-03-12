package ru.max.botframework.ingestion;

import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory monotonic marker state.
 */
public final class InMemoryPollingMarkerState implements PollingMarkerState {
    private final AtomicReference<Long> marker;

    public InMemoryPollingMarkerState(Long initialMarker) {
        this.marker = new AtomicReference<>(initialMarker);
    }

    @Override
    public Long current() {
        return marker.get();
    }

    @Override
    public void advance(Long candidate) {
        if (candidate == null) {
            return;
        }
        marker.updateAndGet(current -> {
            if (current == null || candidate > current) {
                return candidate;
            }
            return current;
        });
    }
}
