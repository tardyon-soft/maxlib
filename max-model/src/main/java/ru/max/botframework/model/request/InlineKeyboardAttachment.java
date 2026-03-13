package ru.max.botframework.model.request;

import java.util.List;
import java.util.Objects;

/**
 * Low-level inline keyboard attachment payload.
 */
public record InlineKeyboardAttachment(List<List<InlineKeyboardButtonRequest>> rows) {
    public InlineKeyboardAttachment {
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("rows must not be empty");
        }
        rows = rows.stream()
                .map(row -> {
                    Objects.requireNonNull(row, "row");
                    if (row.isEmpty()) {
                        throw new IllegalArgumentException("row must not be empty");
                    }
                    return List.copyOf(row);
                })
                .toList();
    }
}
