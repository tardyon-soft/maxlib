package ru.max.botframework.ingestion;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import ru.max.botframework.client.error.MaxSerializationException;
import ru.max.botframework.client.serialization.JsonCodec;
import ru.max.botframework.model.Update;

/**
 * Framework-agnostic default webhook receiver.
 */
public final class DefaultWebhookReceiver implements WebhookReceiver {
    private final WebhookSecretValidator secretValidator;
    private final JsonCodec jsonCodec;
    private final UpdateSink sink;

    public DefaultWebhookReceiver(WebhookSecretValidator secretValidator, JsonCodec jsonCodec, UpdateSink sink) {
        this.secretValidator = Objects.requireNonNull(secretValidator, "secretValidator");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public CompletionStage<WebhookReceiveResult> receive(WebhookRequest request) {
        WebhookRequest nonNullRequest = Objects.requireNonNull(request, "request");

        WebhookSecretValidationResult validationResult =
                secretValidator.validate(nonNullRequest.header(DefaultWebhookSecretValidator.SECRET_HEADER_NAME).orElse(null));
        if (validationResult.status() == WebhookSecretValidationStatus.REJECTED) {
            return CompletableFuture.completedFuture(WebhookReceiveResult.invalidSecret(validationResult));
        }

        Update update;
        try {
            update = jsonCodec.read(new String(nonNullRequest.body(), StandardCharsets.UTF_8), Update.class);
        } catch (MaxSerializationException serializationException) {
            return CompletableFuture.completedFuture(
                    WebhookReceiveResult.badPayload("Unable to deserialize webhook update payload", serializationException)
            );
        } catch (RuntimeException runtimeException) {
            return CompletableFuture.completedFuture(
                    WebhookReceiveResult.internalError("Unexpected webhook payload processing error", runtimeException)
            );
        }

        CompletionStage<UpdateHandlingResult> sinkStage;
        try {
            sinkStage = sink.handle(update);
        } catch (RuntimeException runtimeException) {
            return CompletableFuture.completedFuture(
                    WebhookReceiveResult.internalError("Update sink failed before completion stage creation", runtimeException)
            );
        }

        return sinkStage.handle((result, error) -> {
            if (error != null) {
                Throwable unwrapped = unwrap(error);
                return WebhookReceiveResult.internalError("Update sink completion failed", unwrapped);
            }
            if (result == null || !result.isSuccess()) {
                return WebhookReceiveResult.internalError(
                        "Update sink reported handling failure",
                        result == null ? null : result.error().orElse(null)
                );
            }
            return WebhookReceiveResult.accepted(validationResult);
        });
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }
}
