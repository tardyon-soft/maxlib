package ru.max.botframework.client.error;

/**
 * Generic exception for non-specialized 4xx responses.
 */
public class MaxClientErrorException extends MaxApiException {
    public MaxClientErrorException(int statusCode, String responseBody, String requestMethod, String requestPath) {
        super(statusCode, responseBody, requestMethod, requestPath);
    }
}
