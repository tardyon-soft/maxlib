package ru.max.botframework.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for inline keyboard layouts grouped by rows.
 */
public final class KeyboardBuilder {
    private final List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    public KeyboardBuilder row(InlineKeyboardButton... buttons) {
        Objects.requireNonNull(buttons, "buttons");
        if (buttons.length == 0) {
            throw new IllegalArgumentException("row must contain at least one button");
        }
        ArrayList<InlineKeyboardButton> row = new ArrayList<>(buttons.length);
        for (InlineKeyboardButton button : buttons) {
            row.add(Objects.requireNonNull(button, "button"));
        }
        rows.add(List.copyOf(row));
        return this;
    }

    public KeyboardBuilder row(List<InlineKeyboardButton> buttons) {
        Objects.requireNonNull(buttons, "buttons");
        if (buttons.isEmpty()) {
            throw new IllegalArgumentException("row must contain at least one button");
        }
        ArrayList<InlineKeyboardButton> row = new ArrayList<>(buttons.size());
        for (InlineKeyboardButton button : buttons) {
            row.add(Objects.requireNonNull(button, "button"));
        }
        rows.add(List.copyOf(row));
        return this;
    }

    public InlineKeyboard build() {
        return new InlineKeyboard(rows);
    }
}
