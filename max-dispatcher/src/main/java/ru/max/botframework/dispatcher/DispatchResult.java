package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of dispatcher processing for one update.
 */
public record DispatchResult(
        DispatchStatus status,
        Throwable error
) {
    public DispatchResult {
        Objects.requireNonNull(status, "status");
    }

    public static DispatchResult handled() {
        return new DispatchResult(DispatchStatus.HANDLED, null);
    }

    public static DispatchResult ignored() {
        return new DispatchResult(DispatchStatus.IGNORED, null);
    }

    public static DispatchResult failed(Throwable error) {
        return new DispatchResult(DispatchStatus.FAILED, Objects.requireNonNull(error, "error"));
    }

    public boolean isHandled() {
        return status == DispatchStatus.HANDLED;
    }

    public Optional<Throwable> errorOpt() {
        return Optional.ofNullable(error);
    }
}

