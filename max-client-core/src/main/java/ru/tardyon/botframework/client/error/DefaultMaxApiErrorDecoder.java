package ru.tardyon.botframework.client.error;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.tardyon.botframework.client.http.MaxHttpRequest;
import ru.tardyon.botframework.client.http.MaxHttpResponse;
import ru.tardyon.botframework.client.serialization.SharedObjectMapper;

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
        MaxApiErrorPayload payload = parsePayload(status, responseBody);

        return switch (status) {
            case 400 -> new MaxBadRequestException(responseBody, requestMethod, requestPath, payload);
            case 401 -> new MaxUnauthorizedException(responseBody, requestMethod, requestPath, payload);
            case 403 -> new MaxForbiddenException(responseBody, requestMethod, requestPath, payload);
            case 404 -> new MaxNotFoundException(responseBody, requestMethod, requestPath, payload);
            case 409 -> new MaxConflictException(responseBody, requestMethod, requestPath, payload);
            case 429 -> new MaxRateLimitException(
                    responseBody,
                    requestMethod,
                    requestPath,
                    parseRetryAfterSeconds(response.headers()),
                    payload
            );
            case 503 -> new MaxServiceUnavailableException(responseBody, requestMethod, requestPath, payload);
            default -> {
                if (status >= 500) {
                    yield new MaxServerErrorException(status, responseBody, requestMethod, requestPath, payload);
                }
                if (status >= 400) {
                    yield new MaxClientErrorException(status, responseBody, requestMethod, requestPath, payload);
                }
                yield new MaxApiException(
                        status,
                        responseBody,
                        requestMethod,
                        requestPath,
                        "MAX API request failed with status " + status + " for " + requestMethod + " " + requestPath,
                        payload
                );
            }
        };
    }

    private static MaxApiErrorPayload parsePayload(int status, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new MaxApiErrorPayload(status, null, null, null, "");
        }

        try {
            Map<String, Object> root = SharedObjectMapper.instance()
                    .readValue(responseBody, new TypeReference<Map<String, Object>>() {
                    });

            String errorCode = findFirstString(root, "error_code", "errorCode", "code", "error");
            String message = findFirstString(root, "message", "error_message", "errorMessage", "description");
            Object details = root.get("details");

            if (details == null) {
                Map<String, Object> extra = new LinkedHashMap<>(root);
                extra.remove("status");
                extra.remove("error_code");
                extra.remove("errorCode");
                extra.remove("code");
                extra.remove("error");
                extra.remove("message");
                extra.remove("error_message");
                extra.remove("errorMessage");
                extra.remove("description");
                if (!extra.isEmpty()) {
                    details = extra;
                }
            }

            return new MaxApiErrorPayload(status, errorCode, message, details, responseBody);
        } catch (Exception ignored) {
            return new MaxApiErrorPayload(status, null, null, null, responseBody);
        }
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
                    return values.get(0);
                }
            }
        }
        return null;
    }

    private static String findFirstString(Map<String, Object> root, String... keys) {
        for (String key : keys) {
            Object value = root.get(key);
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                return stringValue;
            }
        }
        return null;
    }
}
