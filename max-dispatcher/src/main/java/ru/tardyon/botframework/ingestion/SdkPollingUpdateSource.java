package ru.tardyon.botframework.ingestion;

import java.util.Objects;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.model.response.GetUpdatesResponse;

/**
 * Polling source implementation backed by {@link MaxBotClient}.
 */
public final class SdkPollingUpdateSource implements PollingUpdateSource {
    private final MaxBotClient client;

    public SdkPollingUpdateSource(MaxBotClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public PollingBatch poll(PollingFetchRequest request) {
        PollingFetchRequest nonNullRequest = Objects.requireNonNull(request, "request");
        GetUpdatesResponse response = client.getUpdates(nonNullRequest.toGetUpdatesRequest());
        return new PollingBatch(response.updates(), response.marker());
    }
}
