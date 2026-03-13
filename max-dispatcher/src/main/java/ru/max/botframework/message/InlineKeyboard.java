package ru.max.botframework.message;

import java.util.List;
import java.util.Objects;
import ru.max.botframework.model.request.InlineKeyboardAttachment;
import ru.max.botframework.model.request.NewMessageAttachment;

/**
 * High-level inline keyboard model.
 */
public final class InlineKeyboard implements KeyboardMarkup {
    private final List<List<InlineKeyboardButton>> rows;

    InlineKeyboard(List<List<InlineKeyboardButton>> rows) {
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("rows must not be empty");
        }
        this.rows = rows.stream()
                .map(row -> {
                    Objects.requireNonNull(row, "row");
                    if (row.isEmpty()) {
                        throw new IllegalArgumentException("row must not be empty");
                    }
                    return List.copyOf(row);
                })
                .toList();
        InlineKeyboardConstraints.validateLayout(this.rows);
    }

    public List<List<InlineKeyboardButton>> rows() {
        return rows;
    }

    NewMessageAttachment toAttachment() {
        List<List<ru.max.botframework.model.request.InlineKeyboardButtonRequest>> lowLevelRows = rows.stream()
                .map(row -> row.stream().map(InlineKeyboardButton::toRequest).toList())
                .toList();
        return NewMessageAttachment.inlineKeyboard(new InlineKeyboardAttachment(lowLevelRows));
    }
}
