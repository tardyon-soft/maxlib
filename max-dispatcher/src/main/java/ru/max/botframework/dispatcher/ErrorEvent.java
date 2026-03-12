package ru.max.botframework.dispatcher;

import java.util.Objects;
import ru.max.botframework.model.Update;

/**
 * Error observer payload.
 *
 * @param update update that caused the error, may be null for non-update failures
 * @param error failure details
 */
public record ErrorEvent(
        Update update,
        Throwable error
) {
    public ErrorEvent {
        Objects.requireNonNull(error, "error");
    }
}

