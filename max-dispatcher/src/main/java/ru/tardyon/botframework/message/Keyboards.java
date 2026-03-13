package ru.tardyon.botframework.message;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Factory entrypoints for keyboard builders.
 */
public final class Keyboards {
    private Keyboards() {
    }

    public static InlineKeyboard inline(UnaryOperator<KeyboardBuilder> spec) {
        Objects.requireNonNull(spec, "spec");
        KeyboardBuilder builder = spec.apply(new KeyboardBuilder());
        return Objects.requireNonNull(builder, "spec must return builder").build();
    }
}
