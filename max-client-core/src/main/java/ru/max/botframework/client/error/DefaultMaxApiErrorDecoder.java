package ru.max.botframework.client.error;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.max.botframework.client.http.MaxHttpRequest;
import ru.max.botframework.client.http.MaxHttpResponse;

/**
 * Default status-code-based implementation for MAX API error mapping.
 */
public final class DefaultMaxApiErrorDecoder implements MaxApiErrorDecoder {

    @Override
    public MaxApiException decode(MaxHttpRequest request, MaxHttpResponse response) {
        int status = response.statusCode();
        String responseBody = new String(response.body(), StandardCharsets.UTF_8);
        String requestMethod = request.method().name();
        String requestPath = request.path();

        return switch (status) {
            case 400 -> new MaxBadRequestException(responseBody, requestMethod, requestPath);
            case 401 -> new MaxUnauthorizedException(responseBody, requestMethod, requestPath);
            case 403 -> new MaxForbiddenException(responseBody, requestMethod, requestPath);
            case 404 -> new MaxNotFoundException(responseBody, requestMethod, requestPath);
            case 409 -> new MaxConflictException(responseBody, requestMethod, requestPath);
            case 429 -> new MaxRateLimitException(
                    responseBody,
                    requestMethod,
                    requestPath,
                    parseRetryAfterSeconds(response.headers())
            );
            case 503 -> new MaxServiceUnavailableException(responseBody, requestMethod, requestPath);
            default -> {
                if (status >= 500) {
                    yield new MaxServerErrorException(status, responseBody, requestMethod, requestPath);
                }
                if (status >= 400) {
                    yield new MaxClientErrorException(status, responseBody, requestMethod, requestPath);
                }
                yield new MaxApiException(status, responseBody, requestMethod, requestPath);
            }
        };
    }

    private static Long parseRetryAfterSeconds(Map<String, List<String>> headers) {
        String value = findHeaderIgnoreCase(headers, "Retry-After");
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
                    return values.getFirst();
                }
            }
        }
        return null;
    }
}
