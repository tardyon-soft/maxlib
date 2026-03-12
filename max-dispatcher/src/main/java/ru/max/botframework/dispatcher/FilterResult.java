package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of one filter execution.
 */
public record FilterResult(
        FilterStatus status,
        Throwable error
) {
    public FilterResult {
        Objects.requireNonNull(status, "status");
    }

    public static FilterResult matched() {
        return new FilterResult(FilterStatus.MATCHED, null);
    }

    public static FilterResult notMatched() {
        return new FilterResult(FilterStatus.NOT_MATCHED, null);
    }

    public static FilterResult failed(Throwable error) {
        return new FilterResult(FilterStatus.FAILED, Objects.requireNonNull(error, "error"));
    }

    public boolean isMatched() {
        return status == FilterStatus.MATCHED;
    }

    public Optional<Throwable> errorOpt() {
        return Optional.ofNullable(error);
    }
}

