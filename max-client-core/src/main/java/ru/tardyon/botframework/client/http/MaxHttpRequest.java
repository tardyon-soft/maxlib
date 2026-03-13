package ru.tardyon.botframework.client.http;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable HTTP request representation used by {@link MaxHttpClient}.
 */
public record MaxHttpRequest(
        HttpMethod method,
        String path,
        Map<String, String> headers,
        byte[] body
) {
    public MaxHttpRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(headers, "headers");
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
