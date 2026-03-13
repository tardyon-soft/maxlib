package ru.tardyon.botframework.ingestion;

import java.util.List;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.model.request.GetUpdatesRequest;

/**
 * Pull request options for one long-polling call.
 */
public record PollingFetchRequest(
        Long marker,
        Integer timeout,
        Integer limit,
        List<UpdateEventType> types
) {
    public PollingFetchRequest {
        types = types == null ? List.of() : List.copyOf(types);
    }

    public static PollingFetchRequest defaults() {
        return new PollingFetchRequest(null, null, null, List.of());
    }

    GetUpdatesRequest toGetUpdatesRequest() {
        return new GetUpdatesRequest(marker, timeout, limit, types);
    }
}
