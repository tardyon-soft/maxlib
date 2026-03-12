package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultWebhookSecretValidatorTest {

    @Test
    void skipsValidationWhenSecretNotConfigured() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator(null);

        WebhookSecretValidationResult result = validator.validate("provided-secret");

        assertEquals(WebhookSecretValidationStatus.SKIPPED_NO_SECRET_CONFIGURED, result.status());
        assertTrue(result.error().isEmpty());
        assertTrue(result.isAccepted());
    }

    @Test
    void treatsBlankConfiguredSecretAsNotConfigured() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("   ");

        WebhookSecretValidationResult result = validator.validate("provided-secret");

        assertEquals(WebhookSecretValidationStatus.SKIPPED_NO_SECRET_CONFIGURED, result.status());
        assertTrue(result.error().isEmpty());
    }

    @Test
    void rejectsWhenSecretConfiguredButHeaderIsMissing() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");

        WebhookSecretValidationResult result = validator.validate(null);

        assertEquals(WebhookSecretValidationStatus.REJECTED, result.status());
        assertEquals(WebhookValidationErrorCode.SECRET_HEADER_MISSING, result.error().orElseThrow().code());
    }

    @Test
    void rejectsWhenSecretConfiguredButHeaderDoesNotMatch() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");

        WebhookSecretValidationResult result = validator.validate("wrong-secret");

        assertEquals(WebhookSecretValidationStatus.REJECTED, result.status());
        assertEquals(WebhookValidationErrorCode.SECRET_MISMATCH, result.error().orElseThrow().code());
    }

    @Test
    void acceptsWhenConfiguredSecretMatchesHeader() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");

        WebhookSecretValidationResult result = validator.validate("expected-secret");

        assertEquals(WebhookSecretValidationStatus.ACCEPTED, result.status());
        assertTrue(result.error().isEmpty());
        assertTrue(result.isAccepted());
    }

    @Test
    void validateAllowsNullHeaderWhenSecretIsNotConfigured() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator(null);
        WebhookSecretValidationResult result = validator.validate(null);
        assertEquals(WebhookSecretValidationStatus.SKIPPED_NO_SECRET_CONFIGURED, result.status());
    }

    @Test
    void validateWithNullHeaderProducesStructuredErrorWhenSecretIsConfigured() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");
        WebhookSecretValidationResult result = validator.validate(null);
        assertEquals(WebhookSecretValidationStatus.REJECTED, result.status());
        assertEquals(WebhookValidationErrorCode.SECRET_HEADER_MISSING, result.error().orElseThrow().code());
    }
}
