package ru.max.botframework.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import ru.max.botframework.client.auth.AuthProvider;

/**
 * Minimal immutable configuration contract for MAX API clients.
 */
public interface MaxApiClientConfig {

    URI baseUri();

    AuthProvider authProvider();

    Duration connectTimeout();

    Duration readTimeout();

    static MaxApiClientConfig of(
            URI baseUri,
            AuthProvider authProvider,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        return new DefaultMaxApiClientConfig(baseUri, authProvider, connectTimeout, readTimeout);
    }

    record DefaultMaxApiClientConfig(
            URI baseUri,
            AuthProvider authProvider,
            Duration connectTimeout,
            Duration readTimeout
    ) implements MaxApiClientConfig {
        public DefaultMaxApiClientConfig {
            Objects.requireNonNull(baseUri, "baseUri");
            Objects.requireNonNull(authProvider, "authProvider");
            Objects.requireNonNull(connectTimeout, "connectTimeout");
            Objects.requireNonNull(readTimeout, "readTimeout");

            if (connectTimeout.isNegative() || connectTimeout.isZero()) {
                throw new IllegalArgumentException("connectTimeout must be positive");
            }
            if (readTimeout.isNegative() || readTimeout.isZero()) {
                throw new IllegalArgumentException("readTimeout must be positive");
            }
        }
    }
}
