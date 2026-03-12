package ru.max.botframework.client.error;

/**
 * Exception for HTTP 409 Conflict responses.
 */
public final class MaxConflictException extends MaxClientErrorException {
    public MaxConflictException(String responseBody, String requestMethod, String requestPath) {
        super(409, responseBody, requestMethod, requestPath);
    }
}
