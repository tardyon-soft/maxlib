package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of handler execution within one observer.
 */
public record HandlerExecutionResult(
        HandlerExecutionStatus status,
        Throwable error
) {
    public HandlerExecutionResult {
        Objects.requireNonNull(status, "status");
    }

    public static HandlerExecutionResult handled() {
        return new HandlerExecutionResult(HandlerExecutionStatus.HANDLED, null);
    }

    public static HandlerExecutionResult ignored() {
        return new HandlerExecutionResult(HandlerExecutionStatus.IGNORED, null);
    }

    public static HandlerExecutionResult failed(Throwable error) {
        return new HandlerExecutionResult(HandlerExecutionStatus.FAILED, Objects.requireNonNull(error, "error"));
    }

    public Optional<Throwable> errorOpt() {
        return Optional.ofNullable(error);
    }
}

