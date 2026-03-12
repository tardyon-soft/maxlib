package ru.max.botframework.dispatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of one filter execution.
 *
 * @param status filter outcome
 * @param enrichment matched enrichment values (only for {@link FilterStatus#MATCHED})
 * @param error optional failure cause (only for {@link FilterStatus#FAILED})
 */
public record FilterResult(
        FilterStatus status,
        Map<String, Object> enrichment,
        Throwable error
) {
    public FilterResult {
        Objects.requireNonNull(status, "status");
        enrichment = enrichment == null ? Map.of() : Map.copyOf(enrichment);
        if (status != FilterStatus.MATCHED && !enrichment.isEmpty()) {
            throw new IllegalArgumentException("enrichment is allowed only for MATCHED result");
        }
    }

    public static FilterResult matched() {
        return new FilterResult(FilterStatus.MATCHED, Map.of(), null);
    }

    /**
     * Matched result with enrichment payload.
     */
    public static FilterResult matched(Map<String, Object> enrichment) {
        return new FilterResult(FilterStatus.MATCHED, enrichment, null);
    }

    public static FilterResult notMatched() {
        return new FilterResult(FilterStatus.NOT_MATCHED, Map.of(), null);
    }

    /**
     * Failed result with execution error.
     */
    public static FilterResult failed(Throwable error) {
        return new FilterResult(FilterStatus.FAILED, Map.of(), Objects.requireNonNull(error, "error"));
    }

    public boolean isMatched() {
        return status == FilterStatus.MATCHED;
    }

    /**
     * Merges enrichment from two matched results.
     *
     * <p>Conflict rule: same key with different values yields {@link #notMatched()}.</p>
     */
    public FilterResult mergeMatched(FilterResult other) {
        Objects.requireNonNull(other, "other");
        if (status != FilterStatus.MATCHED || other.status != FilterStatus.MATCHED) {
            throw new IllegalStateException("mergeMatched supports only MATCHED results");
        }
        if (enrichment.isEmpty()) {
            return matched(other.enrichment);
        }
        if (other.enrichment.isEmpty()) {
            return matched(enrichment);
        }
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>(enrichment);
        for (Map.Entry<String, Object> entry : other.enrichment.entrySet()) {
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue());
                continue;
            }
            Object existing = merged.get(entry.getKey());
            if (!Objects.equals(existing, entry.getValue())) {
                return notMatched();
            }
        }
        return matched(merged);
    }

    public Optional<Throwable> errorOpt() {
        return Optional.ofNullable(error);
    }
}
