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
 * Typed v1 callback action codec using DTO-like transport payload.
 */
public final class TypedV1ScreenActionCodec implements ScreenActionCodec {
    static final String PREFIX = "ui:v1:";
    static final int DEFAULT_MAX_PAYLOAD_LENGTH = 256;

    private final int maxPayloadLength;

    public TypedV1ScreenActionCodec() {
        this(DEFAULT_MAX_PAYLOAD_LENGTH);
    }

    public TypedV1ScreenActionCodec(int maxPayloadLength) {
        if (maxPayloadLength < 32) {
            throw new IllegalArgumentException("maxPayloadLength must be >= 32");
        }
        this.maxPayloadLength = maxPayloadLength;
    }

    @Override
    public ScreenActionCodecMode mode() {
        return ScreenActionCodecMode.TYPED_V1;
    }

    @Override
    public String encode(String action, Map<String, String> args) {
        TypedV1Payload payload = TypedV1Payload.of(action, args == null ? Map.of() : args);
        String encoded = PREFIX + "a=" + encodePart(payload.action()) + encodeArgs(payload.args());
        if (encoded.length() > maxPayloadLength) {
            throw new IllegalArgumentException(
                    "screen callback payload length overflow: " + encoded.length() + " > " + maxPayloadLength
            );
        }
        return encoded;
    }

    @Override
    public Optional<DecodedAction<Map<String, String>>> decode(String payload) {
        if (payload == null || payload.isBlank() || !payload.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String body = payload.substring(PREFIX.length());
        if (body.isBlank()) {
            return Optional.empty();
        }
        if (payload.length() > maxPayloadLength) {
            return Optional.empty();
        }

        Map<String, String> parts = parseQuery(body);
        String action = parts.get("a");
        if (action == null || action.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> args = parseArgs(parts);
        TypedV1Payload dto = new TypedV1Payload(action, args);
        return Optional.of(new DecodedAction<>(dto.action(), dto.args()));
    }

    private static String encodeArgs(Map<String, String> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&");
        args.forEach((key, value) -> joiner.add("arg." + encodePart(key) + "=" + encodePart(value)));
        return "&" + joiner;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                values.put(decodePart(pair), "");
                continue;
            }
            String key = decodePart(pair.substring(0, eq));
            String value = decodePart(pair.substring(eq + 1));
            values.put(key, value);
        }
        return values;
    }

    private static Map<String, String> parseArgs(Map<String, String> queryValues) {
        Map<String, String> args = new LinkedHashMap<>();
        queryValues.forEach((key, value) -> {
            if (!key.startsWith("arg.")) {
                return;
            }
            args.put(key.substring("arg.".length()), value);
        });
        return Map.copyOf(args);
    }

    private static String encodePart(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8);
    }

    private static String decodePart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record TypedV1Payload(String action, Map<String, String> args) {
        private TypedV1Payload {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(args, "args");
            if (action.isBlank()) {
                throw new IllegalArgumentException("action must not be blank");
            }
            args = Map.copyOf(args);
        }

        static TypedV1Payload of(String action, Map<String, String> args) {
            return new TypedV1Payload(action.trim(), Map.copyOf(args));
        }
    }
}
