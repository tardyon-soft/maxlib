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
                buildMessage(statusCode, requestMethod, requestPath, errorPayload),
                errorPayload
        );
    }

    private static String buildMessage(int statusCode, String requestMethod, String requestPath, MaxApiErrorPayload errorPayload) {
        StringBuilder message = new StringBuilder();
        if (requestMethod == null || requestPath == null) {
            message.append("MAX API request failed with status ").append(statusCode);
        } else {
            message.append("MAX API request failed with status ")
                    .append(statusCode)
                    .append(" for ")
                    .append(requestMethod)
                    .append(' ')
                    .append(requestPath);
        }
        appendPayloadSummary(message, errorPayload);
        return message.toString();
    }

    private static void appendPayloadSummary(StringBuilder message, MaxApiErrorPayload errorPayload) {
        if (errorPayload == null) {
            return;
        }
        if (errorPayload.errorCode() != null && !errorPayload.errorCode().isBlank()) {
            message.append(": ").append(errorPayload.errorCode());
            if (errorPayload.message() != null && !errorPayload.message().isBlank()) {
                message.append(" - ").append(errorPayload.message());
            }
            return;
        }
        if (errorPayload.message() != null && !errorPayload.message().isBlank()) {
            message.append(": ").append(errorPayload.message());
        }
    }
}
