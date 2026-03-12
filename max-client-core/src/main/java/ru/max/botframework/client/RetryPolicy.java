package ru.max.botframework.client;

import java.time.Duration;
import java.util.Objects;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.client.http.MaxHttpRequest;
import ru.max.botframework.client.http.MaxHttpResponse;

/**
 * Lightweight retry policy for transport-level retries.
 */
public interface RetryPolicy {

    int maxAttempts();

    Duration delay();

    default Duration delayBeforeAttempt(int attempt) {
        return delay();
    }

    default boolean shouldRetry(MaxHttpRequest request, MaxHttpResponse response, int attempt) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(response, "response");
        return isMethodRetryable(request.method()) && isRetryableStatus(response.statusCode()) && attempt < maxAttempts();
    }

    default boolean shouldRetry(MaxHttpRequest request, RuntimeException error, int attempt) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(error, "error");
        return isMethodRetryable(request.method()) && attempt < maxAttempts();
    }

    default boolean isMethodRetryable(HttpMethod method) {
        return method == HttpMethod.GET;
    }

    default boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 503;
    }

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
