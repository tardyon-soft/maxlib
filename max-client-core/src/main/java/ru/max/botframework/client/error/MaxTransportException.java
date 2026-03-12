package ru.max.botframework.client.error;

/**
 * Exception thrown when low-level HTTP transport fails.
 */
public class MaxTransportException extends MaxClientException {
    public MaxTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
