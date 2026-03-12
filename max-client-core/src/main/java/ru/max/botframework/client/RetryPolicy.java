package ru.max.botframework.client;

import java.time.Duration;
import java.util.Objects;

/**
 * Placeholder contract for transport retry policy.
 *
 * <p>Current foundation layer stores this configuration only;
 * retry behavior will be implemented in a subsequent iteration.</p>
 */
public interface RetryPolicy {

    int maxAttempts();

    Duration delay();

    static RetryPolicy none() {
        return fixed(1, Duration.ZERO);
    }

    static RetryPolicy fixed(int maxAttempts, Duration delay) {
        return new FixedRetryPolicy(maxAttempts, delay);
    }

    record FixedRetryPolicy(int maxAttempts, Duration delay) implements RetryPolicy {
        public FixedRetryPolicy {
            Objects.requireNonNull(delay, "delay");
            if (maxAttempts <= 0) {
                throw new IllegalArgumentException("maxAttempts must be positive");
            }
            if (delay.isNegative()) {
                throw new IllegalArgumentException("delay must not be negative");
            }
        }
    }
}
