package ru.tardyon.botframework.client.error;

/**
 * Exception for HTTP 401 Unauthorized responses.
 */
public final class MaxUnauthorizedException extends MaxClientErrorException {
    public MaxUnauthorizedException(String responseBody, String requestMethod, String requestPath) {
        super(401, responseBody, requestMethod, requestPath);
    }

    public MaxUnauthorizedException(
            String responseBody,
            String requestMethod,
            String requestPath,
            MaxApiErrorPayload errorPayload
    ) {
        super(401, responseBody, requestMethod, requestPath, errorPayload);
    }
}
