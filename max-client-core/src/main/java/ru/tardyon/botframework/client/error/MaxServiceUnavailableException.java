package ru.tardyon.botframework.client.error;

/**
 * Exception for HTTP 503 Service Unavailable responses.
 */
public final class MaxServiceUnavailableException extends MaxServerErrorException {
    public MaxServiceUnavailableException(String responseBody, String requestMethod, String requestPath) {
        super(503, responseBody, requestMethod, requestPath);
    }

    public MaxServiceUnavailableException(
            String responseBody,
            String requestMethod,
            String requestPath,
            MaxApiErrorPayload errorPayload
    ) {
        super(503, responseBody, requestMethod, requestPath, errorPayload);
    }
}
