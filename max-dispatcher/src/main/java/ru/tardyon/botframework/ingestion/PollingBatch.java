package ru.tardyon.botframework.ingestion;

import java.util.List;
import java.util.Objects;
import ru.tardyon.botframework.model.Update;

/**
 * Result of one polling fetch call.
 */
public record PollingBatch(
        List<Update> updates,
        Long nextMarker
) {
    public PollingBatch {
        Objects.requireNonNull(updates, "updates");
        updates = List.copyOf(updates);
    }

    public boolean isEmpty() {
        return updates.isEmpty();
    }
}
