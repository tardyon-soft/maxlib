package ru.tardyon.botframework.client;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.client.auth.AuthorizationHeaderInterceptor;
import ru.tardyon.botframework.client.error.DefaultMaxApiErrorDecoder;
import ru.tardyon.botframework.client.error.MaxApiException;
import ru.tardyon.botframework.client.error.MaxApiErrorDecoder;
import ru.tardyon.botframework.client.error.MaxClientException;
import ru.tardyon.botframework.client.error.MaxTransportException;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.http.MaxHttpRequest;
import ru.tardyon.botframework.client.http.MaxHttpRequestInterceptor;
import ru.tardyon.botframework.client.http.MaxHttpResponse;
import ru.tardyon.botframework.client.serialization.JsonCodec;

/**
 * Default request execution pipeline that bridges typed requests and raw transport.
 */
public final class DefaultMaxBotClient implements MaxBotClient {
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final MaxApiClientConfig config;
    private final MaxHttpClient transport;
    private final JsonCodec jsonCodec;
    private final MaxHttpRequestInterceptor authInterceptor;
    private final MaxApiErrorDecoder errorDecoder;

    public DefaultMaxBotClient(MaxApiClientConfig config, MaxHttpClient transport, JsonCodec jsonCodec) {
        this(
                config,
                transport,
                jsonCodec,
                new AuthorizationHeaderInterceptor(config.authProvider()),
                new DefaultMaxApiErrorDecoder()
        );
    }

    public DefaultMaxBotClient(
            MaxApiClientConfig config,
            MaxHttpClient transport,
            JsonCodec jsonCodec,
            MaxHttpRequestInterceptor authInterceptor
    ) {
        this(config, transport, jsonCodec, authInterceptor, new DefaultMaxApiErrorDecoder());
    }

    public DefaultMaxBotClient(
            MaxApiClientConfig config,
            MaxHttpClient transport,
            JsonCodec jsonCodec,
            MaxHttpRequestInterceptor authInterceptor,
            MaxApiErrorDecoder errorDecoder
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
        this.authInterceptor = Objects.requireNonNull(authInterceptor, "authInterceptor");
        this.errorDecoder = Objects.requireNonNull(errorDecoder, "errorDecoder");
    }

    @Override
    public <T> T execute(MaxRequest<T> request) {
        MaxHttpRequest httpRequest = mapRequest(request);
        MaxHttpRequest authorizedRequest = authInterceptor.intercept(httpRequest);
        MaxHttpResponse response = executeWithRetry(authorizedRequest);

        if (response.statusCode() >= 400) {
            throw errorDecoder.decode(authorizedRequest, response);
        }

        if (request.responseType() == Void.class) {
            return null;
        }

        if (response.body().length == 0) {
            throw new MaxApiException(response.statusCode(), "Empty response body");
        }

        String body = new String(response.body(), StandardCharsets.UTF_8);
        return jsonCodec.read(body, request.responseType());
    }

    private MaxHttpResponse executeWithRetry(MaxHttpRequest request) {
        int attempt = 1;
        while (true) {
            config.rateLimiter().beforeRequest(request);
            try {
                MaxHttpResponse response = transport.execute(request);
                if (response.statusCode() == 429) {
                    config.rateLimiter().onRateLimited(request, response, retryAfterSeconds(response));
                }
                if (response.statusCode() >= 400 && config.retryPolicy().shouldRetry(request, response, attempt)) {
                    sleepBeforeNextAttempt(computeRetryDelay(attempt, response));
                    attempt++;
                    continue;
                }
                return response;
            } catch (MaxTransportException transportException) {
                if (!config.retryPolicy().shouldRetry(request, transportException, attempt)) {
                    throw transportException;
                }
                sleepBeforeNextAttempt(config.retryPolicy().delayBeforeAttempt(attempt));
                attempt++;
            }
        }
    }

    @Override
    public <T> CompletionStage<T> executeAsync(MaxRequest<T> request) {
        return MaxBotClient.super.executeAsync(request);
    }

    private <T> MaxHttpRequest mapRequest(MaxRequest<T> request) {
        byte[] requestBody = serializeBody(request.body());

        Map<String, String> headers = new LinkedHashMap<>(request.headers());
        headers.putIfAbsent(USER_AGENT_HEADER, config.userAgent());

        if (requestBody.length > 0) {
            headers.putIfAbsent(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        }

        return new MaxHttpRequest(
                request.method(),
                buildPath(request.path(), request.queryParameters()),
                headers,
                requestBody
        );
    }

    private byte[] serializeBody(Optional<Object> body) {
        return body
                .map(jsonCodec::write)
                .map(json -> json.getBytes(StandardCharsets.UTF_8))
                .orElseGet(() -> new byte[0]);
    }

    private static String buildPath(String path, Map<String, String> queryParameters) {
        String normalizedPath = normalizePath(path);
        if (queryParameters.isEmpty()) {
            return normalizedPath;
        }

        StringJoiner query = new StringJoiner("&");
        queryParameters.forEach((key, value) -> query.add(encode(key) + "=" + encode(value)));
        return normalizedPath + "?" + query;
    }

    private static String normalizePath(String path) {
        String value = path == null ? "" : path.trim();
        if (value.isEmpty()) {
            return "/";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Duration computeRetryDelay(int attempt, MaxHttpResponse response) {
        Duration retryPolicyDelay = config.retryPolicy().delayBeforeAttempt(attempt);
        if (response.statusCode() != 429) {
            return retryPolicyDelay;
        }

        Long retryAfterSeconds = retryAfterSeconds(response);
        if (retryAfterSeconds == null || retryAfterSeconds <= 0) {
            return retryPolicyDelay;
        }

        Duration retryAfterDelay = Duration.ofSeconds(retryAfterSeconds);
        return retryAfterDelay.compareTo(retryPolicyDelay) > 0 ? retryAfterDelay : retryPolicyDelay;
    }

    private static Long retryAfterSeconds(MaxHttpResponse response) {
        String value = findHeaderIgnoreCase(response.headers(), "Retry-After");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String findHeaderIgnoreCase(Map<String, List<String>> headers, String headerName) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(headerName.toLowerCase(Locale.ROOT))) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
        }
        return null;
    }

    private void sleepBeforeNextAttempt(Duration delay) {
        long delayMillis = delay == null ? 0L : delay.toMillis();
        if (delayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new MaxClientException("Retry interrupted", interruptedException);
        }
    }
}
