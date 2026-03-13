package ru.tardyon.botframework.client.error;

/**
 * Exception for HTTP 409 Conflict responses.
 */
public final class MaxConflictException extends MaxClientErrorException {
    public MaxConflictException(String responseBody, String requestMethod, String requestPath) {
        super(409, responseBody, requestMethod, requestPath);
    }

    public MaxConflictException(
            String responseBody,
            String requestMethod,
            String requestPath,
            MaxApiErrorPayload errorPayload
    ) {
        super(409, responseBody, requestMethod, requestPath, errorPayload);
    }
}
