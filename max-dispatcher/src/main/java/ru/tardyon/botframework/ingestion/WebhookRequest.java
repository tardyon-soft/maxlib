package ru.tardyon.botframework.ingestion;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Framework-agnostic raw webhook request model.
 */
public record WebhookRequest(
        byte[] body,
        Map<String, List<String>> headers
) {
    public WebhookRequest {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(headers, "headers");
        body = body.clone();
        headers = normalizeHeaders(headers);
    }

    public Optional<String> header(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(headers.get(name.toLowerCase(Locale.ROOT)))
                .flatMap(values -> values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0)));
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    private static Map<String, List<String>> normalizeHeaders(Map<String, List<String>> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> normalized = new java.util.LinkedHashMap<>();
        raw.forEach((name, values) -> {
            if (name == null || values == null) {
                return;
            }
            normalized.put(name.toLowerCase(Locale.ROOT), List.copyOf(values));
        });
        return Collections.unmodifiableMap(normalized);
    }
}
