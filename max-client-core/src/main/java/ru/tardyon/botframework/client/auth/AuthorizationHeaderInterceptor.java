package ru.tardyon.botframework.client.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.client.http.MaxHttpRequest;
import ru.tardyon.botframework.client.http.MaxHttpRequestInterceptor;

/**
 * Adds Authorization header to outgoing requests using configured auth provider.
 */
public final class AuthorizationHeaderInterceptor implements MaxHttpRequestInterceptor {
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final AuthProvider authProvider;

    public AuthorizationHeaderInterceptor(AuthProvider authProvider) {
        this.authProvider = Objects.requireNonNull(authProvider, "authProvider");
    }

    @Override
    public MaxHttpRequest intercept(MaxHttpRequest request) {
        Objects.requireNonNull(request, "request");

        if (request.headers().containsKey(AUTHORIZATION_HEADER)) {
            return request;
        }

        String headerValue = authProvider.authorizationHeaderValue();
        if (headerValue == null || headerValue.isBlank()) {
            return request;
        }

        Map<String, String> headers = new LinkedHashMap<>(request.headers());
        headers.put(AUTHORIZATION_HEADER, headerValue);

        return new MaxHttpRequest(
                request.method(),
                request.path(),
                headers,
                request.body()
        );
    }
}
