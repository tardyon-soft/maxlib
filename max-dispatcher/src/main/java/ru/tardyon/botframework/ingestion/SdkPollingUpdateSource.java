package ru.tardyon.botframework.ingestion;

import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.Objects;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.error.MaxTransportException;
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
        ApiGetUpdatesResponse apiResponse;
        try {
            apiResponse = client.getUpdatesApi(nonNullRequest.toGetUpdatesRequest());
        } catch (MaxTransportException transportException) {
            if (isLongPollTimeout(transportException)) {
                return new PollingBatch(java.util.List.of(), nonNullRequest.marker());
            }
            throw transportException;
        }
        if (apiResponse != null && !apiResponse.updates().isEmpty()) {
            return new PollingBatch(apiResponse.updates().stream().map(MaxApiModelMapper::toNormalized).toList(), apiResponse.marker());
        }

        GetUpdatesResponse response;
        try {
            response = client.getUpdates(nonNullRequest.toGetUpdatesRequest());
        } catch (MaxTransportException transportException) {
            if (isLongPollTimeout(transportException)) {
                return new PollingBatch(java.util.List.of(), nonNullRequest.marker());
            }
            throw transportException;
        }
        return new PollingBatch(response == null ? java.util.List.of() : response.updates(), response == null ? null : response.marker());
    }

    private static boolean isLongPollTimeout(MaxTransportException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
