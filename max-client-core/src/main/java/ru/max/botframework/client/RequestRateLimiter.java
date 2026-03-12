package ru.max.botframework.client;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import ru.max.botframework.client.error.MaxClientException;
import ru.max.botframework.client.http.MaxHttpRequest;
import ru.max.botframework.client.http.MaxHttpResponse;

/**
 * Lightweight client-side rate limiter hook for request pacing.
 *
 * <p>Default implementation is a no-op. Clients can plug in custom logic
 * without introducing a heavy resilience framework.</p>
 */
public interface RequestRateLimiter {

    void beforeRequest(MaxHttpRequest request);

    default void onRateLimited(MaxHttpRequest request, MaxHttpResponse response, Long retryAfterSeconds) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(response, "response");
    }

    static RequestRateLimiter noop() {
        return NoopRequestRateLimiter.INSTANCE;
    }

    static RequestRateLimiter cooldown(Duration fallbackDelay) {
        return new CooldownRequestRateLimiter(fallbackDelay);
    }

    final class NoopRequestRateLimiter implements RequestRateLimiter {
        private static final NoopRequestRateLimiter INSTANCE = new NoopRequestRateLimiter();

        private NoopRequestRateLimiter() {
        }

        @Override
        public void beforeRequest(MaxHttpRequest request) {
            Objects.requireNonNull(request, "request");
        }
    }

    final class CooldownRequestRateLimiter implements RequestRateLimiter {
        private final Duration fallbackDelay;
        private final AtomicLong nextAllowedAtMillis = new AtomicLong(0L);

        CooldownRequestRateLimiter(Duration fallbackDelay) {
            this.fallbackDelay = Objects.requireNonNull(fallbackDelay, "fallbackDelay");
            if (fallbackDelay.isNegative()) {
                throw new IllegalArgumentException("fallbackDelay must not be negative");
            }
        }

        @Override
        public void beforeRequest(MaxHttpRequest request) {
            Objects.requireNonNull(request, "request");
            long waitMillis = nextAllowedAtMillis.get() - System.currentTimeMillis();
            if (waitMillis <= 0) {
                return;
            }
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new MaxClientException("Rate limiter wait interrupted", interruptedException);
            }
        }

        @Override
        public void onRateLimited(MaxHttpRequest request, MaxHttpResponse response, Long retryAfterSeconds) {
            RequestRateLimiter.super.onRateLimited(request, response, retryAfterSeconds);

            long delayMillis = fallbackDelay.toMillis();
            if (retryAfterSeconds != null && retryAfterSeconds > 0) {
                delayMillis = retryAfterSeconds * 1_000L;
            }
            if (delayMillis <= 0) {
                return;
            }

            long candidate = System.currentTimeMillis() + delayMillis;
            nextAllowedAtMillis.accumulateAndGet(candidate, Math::max);
        }
    }
}
