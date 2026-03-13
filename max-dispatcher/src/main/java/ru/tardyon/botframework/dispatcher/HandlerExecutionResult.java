package ru.tardyon.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of handler execution within one observer.
 */
public record HandlerExecutionResult(
        HandlerExecutionStatus status,
        Map<String, Object> enrichment,
        Throwable error
) {
    public HandlerExecutionResult {
        Objects.requireNonNull(status, "status");
        enrichment = enrichment == null ? Map.of() : Map.copyOf(enrichment);
        if (status != HandlerExecutionStatus.HANDLED && !enrichment.isEmpty()) {
            throw new IllegalArgumentException("enrichment is allowed only for HANDLED result");
        }
    }

    public static HandlerExecutionResult handled() {
        return new HandlerExecutionResult(HandlerExecutionStatus.HANDLED, Map.of(), null);
    }

    public static HandlerExecutionResult handled(Map<String, Object> enrichment) {
        return new HandlerExecutionResult(HandlerExecutionStatus.HANDLED, enrichment, null);
    }

    public static HandlerExecutionResult ignored() {
        return new HandlerExecutionResult(HandlerExecutionStatus.IGNORED, Map.of(), null);
    }

    public static HandlerExecutionResult failed(Throwable error) {
        return new HandlerExecutionResult(HandlerExecutionStatus.FAILED, Map.of(), Objects.requireNonNull(error, "error"));
    }

    public Optional<Throwable> errorOpt() {
        return Optional.ofNullable(error);
    }
}
