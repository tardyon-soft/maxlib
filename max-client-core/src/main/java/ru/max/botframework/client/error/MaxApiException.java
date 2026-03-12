package ru.max.botframework.client.error;

/**
 * Exception thrown when MAX API responds with non-success HTTP status.
 */
public class MaxApiException extends MaxClientException {
    private final int statusCode;
    private final String responseBody;

    public MaxApiException(int statusCode, String responseBody) {
        super("MAX API request failed with status " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}
