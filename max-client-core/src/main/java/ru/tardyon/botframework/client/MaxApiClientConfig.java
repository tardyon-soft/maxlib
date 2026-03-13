package ru.tardyon.botframework.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import ru.tardyon.botframework.client.auth.AuthProvider;

/**
 * Immutable builder-based configuration contract for MAX API clients.
 */
public interface MaxApiClientConfig {

    URI DEFAULT_BASE_URI = URI.create("https://api.max.ru");
    Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    String DEFAULT_USER_AGENT = "max-bot-framework-client";

    URI baseUri();

    String token();

    Duration connectTimeout();

    Duration readTimeout();

    String userAgent();

    RetryPolicy retryPolicy();

    RequestRateLimiter rateLimiter();

    default AuthProvider authProvider() {
        return () -> authorizationHeaderValue(token());
    }

    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private URI baseUri = DEFAULT_BASE_URI;
        private String token;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private String userAgent = DEFAULT_USER_AGENT;
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private RequestRateLimiter rateLimiter = RequestRateLimiter.noop();

        public Builder baseUri(URI baseUri) {
            this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            Objects.requireNonNull(baseUrl, "baseUrl");
            this.baseUri = URI.create(baseUrl);
            return this;
        }

        public Builder token(String token) {
            this.token = Objects.requireNonNull(token, "token");
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
            return this;
        }

        public Builder rateLimiter(RequestRateLimiter rateLimiter) {
            this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
            return this;
        }

        public MaxApiClientConfig build() {
            return new DefaultMaxApiClientConfig(
                    baseUri,
                    token,
                    connectTimeout,
                    readTimeout,
                    userAgent,
                    retryPolicy,
                    rateLimiter
            );
        }
    }

    record DefaultMaxApiClientConfig(
            URI baseUri,
            String token,
            Duration connectTimeout,
            Duration readTimeout,
            String userAgent,
            RetryPolicy retryPolicy,
            RequestRateLimiter rateLimiter
    ) implements MaxApiClientConfig {
        public DefaultMaxApiClientConfig {
            Objects.requireNonNull(baseUri, "baseUri");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(connectTimeout, "connectTimeout");
            Objects.requireNonNull(readTimeout, "readTimeout");
            Objects.requireNonNull(userAgent, "userAgent");
            Objects.requireNonNull(retryPolicy, "retryPolicy");
            Objects.requireNonNull(rateLimiter, "rateLimiter");

            if (connectTimeout.isNegative() || connectTimeout.isZero()) {
                throw new IllegalArgumentException("connectTimeout must be positive");
            }
            if (readTimeout.isNegative() || readTimeout.isZero()) {
                throw new IllegalArgumentException("readTimeout must be positive");
            }
            if (token.isBlank()) {
                throw new IllegalArgumentException("token must not be blank");
            }
            if (userAgent.isBlank()) {
                throw new IllegalArgumentException("userAgent must not be blank");
            }
        }
    }

    private static String authorizationHeaderValue(String token) {
        String normalized = token.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return normalized;
        }
        return "Bearer " + normalized;
    }
}
