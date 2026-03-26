package ru.tardyon.botframework.screen;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

final class ScreenCallbackCodec {
    private ScreenCallbackCodec() {
    }

    static String navBack() {
        return "ui:nav:back";
    }

    static String action(String action, Map<String, String> args) {
        String base = "ui:act:" + action;
        if (args == null || args.isEmpty()) {
            return base;
        }
        StringJoiner joiner = new StringJoiner("&");
        args.forEach((k, v) -> joiner.add(encode(k) + "=" + encode(v)));
        return base + "?" + joiner;
    }

    static Optional<ParsedScreenCallback> parse(String data) {
        if (data == null || data.isBlank() || !data.startsWith("ui:")) {
            return Optional.empty();
        }
        if ("ui:nav:back".equals(data)) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.NAV_BACK, null, Map.of()));
        }
        if ("ui:nav:home".equals(data)) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.NAV_HOME, null, Map.of()));
        }
        if ("ui:nav:refresh".equals(data)) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.NAV_REFRESH, null, Map.of()));
        }
        if (!data.startsWith("ui:act:")) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.UNKNOWN, null, Map.of()));
        }

        String raw = data.substring("ui:act:".length());
        int queryIndex = raw.indexOf('?');
        String action = queryIndex < 0 ? raw : raw.substring(0, queryIndex);
        if (action.isBlank()) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.UNKNOWN, null, Map.of()));
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
                    args.put(decode(pair), "");
                    continue;
                }
                String key = decode(pair.substring(0, eqIndex));
                String value = decode(pair.substring(eqIndex + 1));
                args.put(key, value);
            }
        }
        return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.ACTION, action, Map.copyOf(args)));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record ParsedScreenCallback(Kind kind, String action, Map<String, String> args) {
        enum Kind {
            ACTION,
            NAV_BACK,
            NAV_HOME,
            NAV_REFRESH,
            UNKNOWN
        }
    }
}
