package ru.max.botframework.client.error;

/**
 * Structured MAX API error payload normalized from an HTTP error response.
 */
public record MaxApiErrorPayload(
        int status,
        String errorCode,
        String message,
        Object details,
        String rawBody
) {
}
