package ru.max.botframework.client.error;

/**
 * Exception for HTTP 400 Bad Request responses.
 */
public final class MaxBadRequestException extends MaxClientErrorException {
    public MaxBadRequestException(String responseBody, String requestMethod, String requestPath) {
        super(400, responseBody, requestMethod, requestPath);
    }
}
