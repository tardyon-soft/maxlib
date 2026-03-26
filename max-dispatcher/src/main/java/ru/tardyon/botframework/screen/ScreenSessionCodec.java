package ru.tardyon.botframework.screen;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ScreenSessionCodec {
    private ScreenSessionCodec() {
    }

    static Map<String, Object> encode(ScreenSession session) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("scopeId", session.scopeId());
        root.put("rootMessageId", session.rootMessageId());
        root.put("updatedAt", session.updatedAt().toEpochMilli());

        ArrayList<Map<String, Object>> stack = new ArrayList<>();
        for (ScreenStackEntry entry : session.stack()) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("screenId", entry.screenId());
            item.put("params", entry.params());
            item.put("pushedAt", entry.pushedAt().toEpochMilli());
            stack.add(item);
        }
        root.put("stack", stack);
        return Map.copyOf(root);
    }

    @SuppressWarnings("unchecked")
    static Optional<ScreenSession> decode(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        try {
            String scopeId = stringValue(map.get("scopeId"));
            String rootMessageId = nullableString(map.get("rootMessageId"));
            Instant updatedAt = instantValue(map.get("updatedAt"));

            ArrayList<ScreenStackEntry> stack = new ArrayList<>();
            Object stackRaw = map.get("stack");
            if (stackRaw instanceof List<?> items) {
                for (Object itemRaw : items) {
                    if (!(itemRaw instanceof Map<?, ?> itemMap)) {
                        continue;
                    }
                    String screenId = stringValue(itemMap.get("screenId"));
                    Map<String, Object> params = itemMap.get("params") instanceof Map<?, ?> p
                            ? (Map<String, Object>) p
                            : Map.of();
                    Instant pushedAt = instantValue(itemMap.get("pushedAt"));
                    stack.add(new ScreenStackEntry(screenId, params, pushedAt));
                }
            }
            return Optional.of(new ScreenSession(scopeId, stack, rootMessageId, updatedAt));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static String stringValue(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Missing required string value");
        }
        String value = raw.toString().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("String value must not be blank");
        }
        return value;
    }

    private static String nullableString(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private static Instant instantValue(Object raw) {
        if (raw instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return Instant.parse(stringValue(raw));
    }
}
