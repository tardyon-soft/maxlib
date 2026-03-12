package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;

class DefaultWebhookSecretValidatorTest {

    @Test
    void skipsValidationWhenSecretNotConfigured() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator(null);

        WebhookSecretValidationResult result = validator.validate(payload("provided-secret"));

        assertEquals(WebhookSecretValidationStatus.SKIPPED_NO_SECRET_CONFIGURED, result.status());
        assertTrue(result.error().isEmpty());
        assertTrue(result.isAccepted());
    }

    @Test
    void treatsBlankConfiguredSecretAsNotConfigured() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("   ");

        WebhookSecretValidationResult result = validator.validate(payload("provided-secret"));

        assertEquals(WebhookSecretValidationStatus.SKIPPED_NO_SECRET_CONFIGURED, result.status());
        assertTrue(result.error().isEmpty());
    }

    @Test
    void rejectsWhenSecretConfiguredButHeaderIsMissing() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");

        WebhookSecretValidationResult result = validator.validate(payload(null));

        assertEquals(WebhookSecretValidationStatus.REJECTED, result.status());
        assertEquals(WebhookValidationErrorCode.SECRET_HEADER_MISSING, result.error().orElseThrow().code());
    }

    @Test
    void rejectsWhenSecretConfiguredButHeaderDoesNotMatch() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");

        WebhookSecretValidationResult result = validator.validate(payload("wrong-secret"));

        assertEquals(WebhookSecretValidationStatus.REJECTED, result.status());
        assertEquals(WebhookValidationErrorCode.SECRET_MISMATCH, result.error().orElseThrow().code());
    }

    @Test
    void acceptsWhenConfiguredSecretMatchesHeader() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");

        WebhookSecretValidationResult result = validator.validate(payload("expected-secret"));

        assertEquals(WebhookSecretValidationStatus.ACCEPTED, result.status());
        assertTrue(result.error().isEmpty());
        assertTrue(result.isAccepted());
    }

    @Test
    void validateRequiresPayload() {
        DefaultWebhookSecretValidator validator = new DefaultWebhookSecretValidator("expected-secret");
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }

    private static WebhookUpdatePayload payload(String secretHeader) {
        Update update = new Update(
                new UpdateId("upd-1"),
                UpdateType.MESSAGE,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        return new WebhookUpdatePayload(update, secretHeader);
    }
}
