package ru.tardyon.botframework.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Default validator for MAX webhook secret header.
 */
public final class DefaultWebhookSecretValidator implements WebhookSecretValidator {
    public static final String SECRET_HEADER_NAME = "X-Max-Bot-Api-Secret";

    private final String expectedSecret;

    public DefaultWebhookSecretValidator(String expectedSecret) {
        this.expectedSecret = normalize(expectedSecret);
    }

    @Override
    public WebhookSecretValidationResult validate(String secretHeader) {
        if (expectedSecret == null) {
            return WebhookSecretValidationResult.skippedNoSecretConfigured();
        }

        String provided = normalize(secretHeader);
        if (provided == null) {
            return WebhookSecretValidationResult.rejected(
                    WebhookValidationErrorCode.SECRET_HEADER_MISSING,
                    SECRET_HEADER_NAME + " header is required when webhook secret is configured"
            );
        }

        if (!constantTimeEquals(expectedSecret, provided)) {
            return WebhookSecretValidationResult.rejected(
                    WebhookValidationErrorCode.SECRET_MISMATCH,
                    SECRET_HEADER_NAME + " header value does not match configured webhook secret"
            );
        }

        return WebhookSecretValidationResult.accepted();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
