package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import ru.tardyon.botframework.model.Update;

/**
 * Error observer payload.
 *
 * @param update update that caused the error, may be null for non-update failures
 * @param error failure details
 * @param type runtime dispatch error category
 */
public record ErrorEvent(
        Update update,
        Throwable error,
        RuntimeDispatchErrorType type
) {
    public ErrorEvent {
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(type, "type");
    }
}
