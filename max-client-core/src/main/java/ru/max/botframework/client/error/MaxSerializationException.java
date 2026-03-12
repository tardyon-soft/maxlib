package ru.max.botframework.client.error;

/**
 * Exception thrown when request/response JSON payload cannot be mapped.
 */
public class MaxSerializationException extends MaxClientException {
    public MaxSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
