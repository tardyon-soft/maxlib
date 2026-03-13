package ru.max.botframework.testkit;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;

/**
 * Immutable snapshot of one {@link MaxRequest} executed by {@link RecordingMaxBotClient}.
 */
public record CapturedApiCall(
        HttpMethod method,
        String path,
        Map<String, String> query,
        Optional<Object> body,
        MaxRequest<?> request
) {
    public CapturedApiCall {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        query = Map.copyOf(Objects.requireNonNull(query, "query"));
        body = Objects.requireNonNull(body, "body");
        Objects.requireNonNull(request, "request");
    }
}
