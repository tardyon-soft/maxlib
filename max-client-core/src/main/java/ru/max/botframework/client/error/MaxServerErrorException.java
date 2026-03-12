package ru.max.botframework.client.error;

/**
 * Generic exception for non-specialized 5xx responses.
 */
public class MaxServerErrorException extends MaxApiException {
    public MaxServerErrorException(int statusCode, String responseBody, String requestMethod, String requestPath) {
        super(statusCode, responseBody, requestMethod, requestPath);
    }
}
