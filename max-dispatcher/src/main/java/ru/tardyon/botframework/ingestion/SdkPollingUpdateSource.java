package ru.tardyon.botframework.ingestion;

import java.util.Objects;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.model.mapping.MaxApiModelMapper;
import ru.tardyon.botframework.model.response.GetUpdatesResponse;
import ru.tardyon.botframework.model.transport.ApiGetUpdatesResponse;

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
        ApiGetUpdatesResponse apiResponse = client.getUpdatesApi(nonNullRequest.toGetUpdatesRequest());
        if (apiResponse != null && !apiResponse.updates().isEmpty()) {
            return new PollingBatch(apiResponse.updates().stream().map(MaxApiModelMapper::toNormalized).toList(), apiResponse.marker());
        }

        GetUpdatesResponse response = client.getUpdates(nonNullRequest.toGetUpdatesRequest());
        return new PollingBatch(response == null ? java.util.List.of() : response.updates(), response == null ? null : response.marker());
    }
}
