package ru.max.botframework.client.error;

/**
 * Exception for HTTP 404 Not Found responses.
 */
public final class MaxNotFoundException extends MaxClientErrorException {
    public MaxNotFoundException(String responseBody, String requestMethod, String requestPath) {
        super(404, responseBody, requestMethod, requestPath);
    }
}
