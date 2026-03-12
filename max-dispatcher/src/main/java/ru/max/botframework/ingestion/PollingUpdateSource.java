package ru.max.botframework.ingestion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Pull-based update source for MAX long polling transport.
 */
public interface PollingUpdateSource extends AutoCloseable {

    PollingBatch poll(PollingFetchRequest request);

    default CompletionStage<PollingBatch> pollAsync(PollingFetchRequest request) {
        return CompletableFuture.supplyAsync(() -> poll(request));
    }

    @Override
    default void close() {
        // no-op by default
    }
}
