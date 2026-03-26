package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.Objects;

/**
 * Logical screen action button.
 */
public record ScreenButton(
        String text,
        String action,
        Map<String, String> args
) {
    public ScreenButton {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(args, "args");
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        args = Map.copyOf(args);
    }

    public static ScreenButton of(String text, String action) {
        return new ScreenButton(text, action, Map.of());
    }
}
