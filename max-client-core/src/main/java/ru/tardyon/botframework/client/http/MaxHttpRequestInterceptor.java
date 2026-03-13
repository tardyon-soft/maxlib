package ru.tardyon.botframework.client.http;

/**
 * Intercepts and transforms outgoing HTTP requests before transport execution.
 */
@FunctionalInterface
public interface MaxHttpRequestInterceptor {
    MaxHttpRequest intercept(MaxHttpRequest request);
}
