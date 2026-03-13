package ru.tardyon.botframework.client.error;

/**
 * Base runtime exception for MAX client-core failures.
 */
public class MaxClientException extends RuntimeException {
    public MaxClientException(String message) {
        super(message);
    }

    public MaxClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
