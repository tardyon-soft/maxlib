package ru.tardyon.botframework.screen;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Legacy callback action codec compatible with existing {@code ui:act:<action>?k=v} payload format.
 */
public final class LegacyStringScreenActionCodec implements ScreenActionCodec {
    static final String PREFIX = "ui:act:";

    @Override
    public ScreenActionCodecMode mode() {
        return ScreenActionCodecMode.LEGACY_STRING;
    }

    @Override
    public String encode(String action, Map<String, String> args) {
        String normalizedAction = normalizeAction(action);
        String base = PREFIX + normalizedAction;
        if (args == null || args.isEmpty()) {
            return base;
        }
        StringJoiner joiner = new StringJoiner("&");
        args.forEach((k, v) -> joiner.add(encodePart(k) + "=" + encodePart(v)));
        return base + "?" + joiner;
    }

    @Override
    public Optional<DecodedAction<Map<String, String>>> decode(String payload) {
        if (payload == null || payload.isBlank() || !payload.startsWith(PREFIX)) {
            return Optional.empty();
        }

        String raw = payload.substring(PREFIX.length());
        int queryIndex = raw.indexOf('?');
        String action = queryIndex < 0 ? raw : raw.substring(0, queryIndex);
        if (action.isBlank()) {
            return Optional.empty();
        }

        Map<String, String> args = new LinkedHashMap<>();
        if (queryIndex >= 0 && queryIndex + 1 < raw.length()) {
            String query = raw.substring(queryIndex + 1);
            for (String pair : query.split("&")) {
                if (pair.isBlank()) {
                    continue;
                }
                int eqIndex = pair.indexOf('=');
                if (eqIndex < 0) {
                    args.put(decodePart(pair), "");
                    continue;
                }
                String key = decodePart(pair.substring(0, eqIndex));
                String value = decodePart(pair.substring(eqIndex + 1));
                args.put(key, value);
            }
        }

        return Optional.of(new DecodedAction<>(action, Map.copyOf(args)));
    }

    private static String normalizeAction(String action) {
        Objects.requireNonNull(action, "action");
        String value = action.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        return value;
    }

    private static String encodePart(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8);
    }

    private static String decodePart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}

