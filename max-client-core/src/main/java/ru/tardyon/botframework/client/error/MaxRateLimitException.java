package ru.tardyon.botframework.client.error;

/**
 * Exception for HTTP 429 Too Many Requests responses.
 */
public final class MaxRateLimitException extends MaxClientErrorException {
    private final Long retryAfterSeconds;

    public MaxRateLimitException(
            String responseBody,
            String requestMethod,
            String requestPath,
            Long retryAfterSeconds
    ) {
        super(429, responseBody, requestMethod, requestPath);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public MaxRateLimitException(
            String responseBody,
            String requestMethod,
            String requestPath,
            Long retryAfterSeconds,
            MaxApiErrorPayload errorPayload
    ) {
        super(429, responseBody, requestMethod, requestPath, errorPayload);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
