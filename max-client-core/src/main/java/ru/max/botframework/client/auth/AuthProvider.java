package ru.max.botframework.client.auth;

/**
 * Provides authorization value for MAX API requests.
 */
@FunctionalInterface
public interface AuthProvider {
    String authorizationHeaderValue();
}
