package ru.max.botframework.client;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletionStage;
import ru.max.botframework.client.error.MaxApiException;
import ru.max.botframework.client.http.MaxHttpClient;
import ru.max.botframework.client.http.MaxHttpRequest;
import ru.max.botframework.client.http.MaxHttpResponse;
import ru.max.botframework.client.serialization.JsonCodec;

/**
 * Default request execution pipeline that bridges typed requests and raw transport.
 */
public final class DefaultMaxBotClient implements MaxBotClient {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final MaxApiClientConfig config;
    private final MaxHttpClient transport;
    private final JsonCodec jsonCodec;

    public DefaultMaxBotClient(MaxApiClientConfig config, MaxHttpClient transport, JsonCodec jsonCodec) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
    }

    @Override
    public <T> T execute(MaxRequest<T> request) {
        MaxHttpRequest httpRequest = mapRequest(request);
        MaxHttpResponse response = transport.execute(httpRequest);

        if (response.statusCode() >= 400) {
            throw new MaxApiException(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
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

    @Override
    public <T> CompletionStage<T> executeAsync(MaxRequest<T> request) {
        return MaxBotClient.super.executeAsync(request);
    }

    private <T> MaxHttpRequest mapRequest(MaxRequest<T> request) {
        byte[] requestBody = serializeBody(request.body());

        Map<String, String> headers = new LinkedHashMap<>(request.headers());
        headers.putIfAbsent(USER_AGENT_HEADER, config.userAgent());
        headers.putIfAbsent(AUTHORIZATION_HEADER, authorizationHeaderValue(config.token()));

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

    private static String authorizationHeaderValue(String token) {
        String normalizedToken = token.trim();
        if (normalizedToken.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return normalizedToken;
        }
        return "Bearer " + normalizedToken;
    }
}
