package ru.max.botframework.client.error;

/**
 * Exception thrown when MAX API responds with non-success HTTP status.
 */
public class MaxApiException extends MaxClientException {
    private final int statusCode;
    private final String responseBody;
    private final String requestMethod;
    private final String requestPath;

    public MaxApiException(int statusCode, String responseBody) {
        this(statusCode, responseBody, null, null);
    }

    public MaxApiException(int statusCode, String responseBody, String requestMethod, String requestPath) {
        this(statusCode, responseBody, requestMethod, requestPath, buildMessage(statusCode, requestMethod, requestPath));
    }

    public MaxApiException(
            int statusCode,
            String responseBody,
            String requestMethod,
            String requestPath,
            String message
    ) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody == null ? "" : responseBody;
        this.requestMethod = requestMethod;
        this.requestPath = requestPath;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }

    public String requestMethod() {
        return requestMethod;
    }

    public String requestPath() {
        return requestPath;
    }

    private static String buildMessage(int statusCode, String requestMethod, String requestPath) {
        if (requestMethod == null || requestPath == null) {
            return "MAX API request failed with status " + statusCode;
        }
        return "MAX API request failed with status " + statusCode + " for " + requestMethod + " " + requestPath;
    }
}
