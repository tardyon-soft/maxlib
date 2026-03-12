package ru.max.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of dispatcher processing for one update.
 *
 * @param status dispatch status
 * @param enrichment optional filter/matching enrichment, present for {@link DispatchStatus#HANDLED}
 * @param error optional runtime error, present only for {@link DispatchStatus#FAILED}
 */
public record DispatchResult(
        DispatchStatus status,
        Map<String, Object> enrichment,
        Throwable error
) {
    public DispatchResult {
        Objects.requireNonNull(status, "status");
        enrichment = enrichment == null ? Map.of() : Map.copyOf(enrichment);
        if (status != DispatchStatus.HANDLED && !enrichment.isEmpty()) {
            throw new IllegalArgumentException("enrichment is allowed only for HANDLED result");
        }
    }

    public static DispatchResult handled() {
        return new DispatchResult(DispatchStatus.HANDLED, Map.of(), null);
    }

    public static DispatchResult handled(Map<String, Object> enrichment) {
        return new DispatchResult(DispatchStatus.HANDLED, enrichment, null);
    }

    public static DispatchResult ignored() {
        return new DispatchResult(DispatchStatus.IGNORED, Map.of(), null);
    }

    public static DispatchResult failed(Throwable error) {
        return new DispatchResult(DispatchStatus.FAILED, Map.of(), Objects.requireNonNull(error, "error"));
    }

    public boolean isHandled() {
        return status == DispatchStatus.HANDLED;
    }

    /**
     * Optional runtime error details for {@link DispatchStatus#FAILED}.
     */
    public Optional<Throwable> errorOpt() {
        return Optional.ofNullable(error);
    }
}
