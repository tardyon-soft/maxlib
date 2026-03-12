package ru.max.botframework.client.pagination;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Marker-based page response abstraction.
 *
 * @param <T> item type
 */
public record MarkerPage<T>(List<T> items, String nextMarker) implements Page<T> {

    public MarkerPage {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(nextMarker, "nextMarker");
        items = List.copyOf(items);
    }

    @Override
    public boolean hasNext() {
        return !nextMarker.isBlank();
    }

    public Optional<MarkerPageRequest> nextPageRequest(int limit) {
        if (!hasNext()) {
            return Optional.empty();
        }
        return Optional.of(new MarkerPageRequest(limit, nextMarker));
    }

    public Optional<MarkerPageRequest> nextPageRequest(MarkerPageRequest currentRequest) {
        Objects.requireNonNull(currentRequest, "currentRequest");
        return nextPageRequest(currentRequest.limit());
    }
}
