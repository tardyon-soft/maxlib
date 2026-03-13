package ru.tardyon.botframework.client.error;

/**
 * Exception for HTTP 403 Forbidden responses.
 */
public final class MaxForbiddenException extends MaxClientErrorException {
    public MaxForbiddenException(String responseBody, String requestMethod, String requestPath) {
        super(403, responseBody, requestMethod, requestPath);
    }

    public MaxForbiddenException(
            String responseBody,
            String requestMethod,
            String requestPath,
            MaxApiErrorPayload errorPayload
    ) {
        super(403, responseBody, requestMethod, requestPath, errorPayload);
    }
}
