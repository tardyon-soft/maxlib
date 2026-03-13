package ru.tardyon.botframework.client.error;

/**
 * Generic exception for non-specialized 4xx responses.
 */
public class MaxClientErrorException extends MaxApiException {
    public MaxClientErrorException(int statusCode, String responseBody, String requestMethod, String requestPath) {
        super(statusCode, responseBody, requestMethod, requestPath);
    }

    public MaxClientErrorException(
            int statusCode,
            String responseBody,
            String requestMethod,
            String requestPath,
            MaxApiErrorPayload errorPayload
    ) {
        super(
                statusCode,
                responseBody,
                requestMethod,
                requestPath,
                buildMessage(statusCode, requestMethod, requestPath),
                errorPayload
        );
    }

    private static String buildMessage(int statusCode, String requestMethod, String requestPath) {
        if (requestMethod == null || requestPath == null) {
            return "MAX API request failed with status " + statusCode;
        }
        return "MAX API request failed with status " + statusCode + " for " + requestMethod + " " + requestPath;
    }
}
