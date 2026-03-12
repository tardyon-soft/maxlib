package ru.max.botframework.ingestion;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
    private final UpdatePipeline pipeline;

    public DefaultWebhookReceiver(WebhookSecretValidator secretValidator, JsonCodec jsonCodec, UpdateSink sink) {
        this(secretValidator, jsonCodec, new DefaultUpdatePipeline(sink));
    }

    public DefaultWebhookReceiver(WebhookSecretValidator secretValidator, JsonCodec jsonCodec, UpdatePipeline pipeline) {
        this.secretValidator = Objects.requireNonNull(secretValidator, "secretValidator");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
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

        return pipeline.process(update, UpdatePipelineContext.WEBHOOK).handle((pipelineResult, error) -> {
            if (error != null) {
                return WebhookReceiveResult.internalError("Update pipeline completion failed", error);
            }
            if (pipelineResult == null || !pipelineResult.isAccepted()) {
                return WebhookReceiveResult.internalError(
                        "Update pipeline reported handling failure",
                        pipelineResult == null ? null : pipelineResult.error().orElse(null)
                );
            }
            return WebhookReceiveResult.accepted(validationResult);
        });
    }
}
