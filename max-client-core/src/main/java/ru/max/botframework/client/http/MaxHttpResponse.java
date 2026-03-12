package ru.max.botframework.client.http;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable HTTP response representation for MAX API transport layer.
 */
public record MaxHttpResponse(
        int statusCode,
        Map<String, List<String>> headers,
        byte[] body
) {
    public MaxHttpResponse {
        if (statusCode < 100) {
            throw new IllegalArgumentException("statusCode must be a valid HTTP code");
        }
        Objects.requireNonNull(headers, "headers");
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
