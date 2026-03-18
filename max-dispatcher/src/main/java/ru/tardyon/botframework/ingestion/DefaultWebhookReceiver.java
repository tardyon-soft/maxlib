package ru.tardyon.botframework.ingestion;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.client.error.MaxSerializationException;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.model.mapping.MaxApiModelMapper;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.transport.ApiUpdate;

/**
 * Framework-agnostic default webhook receiver.
 */
public final class DefaultWebhookReceiver implements WebhookReceiver {
    private static final Logger log = LoggerFactory.getLogger(DefaultWebhookReceiver.class);
    private final WebhookSecretValidator secretValidator;
    private final JsonCodec jsonCodec;
    private final UpdatePipeline pipeline;
    private final Semaphore inFlight;

    public DefaultWebhookReceiver(WebhookSecretValidator secretValidator, JsonCodec jsonCodec, UpdateConsumer consumer) {
        this(secretValidator, jsonCodec, wrap(consumer), WebhookReceiverConfig.defaults());
    }

    public DefaultWebhookReceiver(WebhookSecretValidator secretValidator, JsonCodec jsonCodec, UpdateSink sink) {
        this(secretValidator, jsonCodec, new DefaultUpdatePipeline(sink), WebhookReceiverConfig.defaults());
    }

    public DefaultWebhookReceiver(WebhookSecretValidator secretValidator, JsonCodec jsonCodec, UpdatePipeline pipeline) {
        this(secretValidator, jsonCodec, pipeline, WebhookReceiverConfig.defaults());
    }

    public DefaultWebhookReceiver(
            WebhookSecretValidator secretValidator,
            JsonCodec jsonCodec,
            UpdateConsumer consumer,
            WebhookReceiverConfig config
    ) {
        this(secretValidator, jsonCodec, wrap(consumer), config);
    }

    public DefaultWebhookReceiver(
            WebhookSecretValidator secretValidator,
            JsonCodec jsonCodec,
            UpdateSink sink,
            WebhookReceiverConfig config
    ) {
        this(secretValidator, jsonCodec, new DefaultUpdatePipeline(sink), config);
    }

    public DefaultWebhookReceiver(
            WebhookSecretValidator secretValidator,
            JsonCodec jsonCodec,
            UpdatePipeline pipeline,
            WebhookReceiverConfig config
    ) {
        this.secretValidator = Objects.requireNonNull(secretValidator, "secretValidator");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        WebhookReceiverConfig receiverConfig = Objects.requireNonNull(config, "config");
        this.inFlight = new Semaphore(receiverConfig.maxInFlightRequests());
    }

    @Override
    public CompletionStage<WebhookReceiveResult> receive(WebhookRequest request) {
        WebhookRequest nonNullRequest = Objects.requireNonNull(request, "request");
        log.debug("Webhook request received: bodyBytes={}, headers={}", nonNullRequest.body().length, nonNullRequest.headers().keySet());
        if (!inFlight.tryAcquire()) {
            log.debug("Webhook request rejected: receiver overloaded");
            return CompletableFuture.completedFuture(WebhookReceiveResult.overloaded("Webhook receiver is overloaded"));
        }

        try {
            WebhookSecretValidationResult validationResult =
                    secretValidator.validate(nonNullRequest.header(DefaultWebhookSecretValidator.SECRET_HEADER_NAME).orElse(null));
            if (validationResult.status() == WebhookSecretValidationStatus.REJECTED) {
                log.debug("Webhook request rejected: invalid secret");
                return completed(WebhookReceiveResult.invalidSecret(validationResult));
            }

            Update update;
            try {
                String payload = new String(nonNullRequest.body(), StandardCharsets.UTF_8);
                ApiUpdate apiUpdate = jsonCodec.read(payload, ApiUpdate.class);
                if (isMeaningfulApiUpdate(apiUpdate)) {
                    update = MaxApiModelMapper.toNormalized(apiUpdate);
                } else {
                    update = jsonCodec.read(payload, Update.class);
                }
            } catch (MaxSerializationException serializationException) {
                log.debug("Webhook request rejected: bad payload", serializationException);
                return completed(WebhookReceiveResult.badPayload("Unable to deserialize webhook update payload", serializationException));
            } catch (RuntimeException runtimeException) {
                log.debug("Webhook request failed during payload processing", runtimeException);
                return completed(WebhookReceiveResult.internalError("Unexpected webhook payload processing error", runtimeException));
            }

            return pipeline.process(update, UpdatePipelineContext.WEBHOOK).handle((pipelineResult, error) -> {
                if (error != null) {
                    log.debug("Webhook pipeline failed for updateId={}", update.updateId().value(), error);
                    return WebhookReceiveResult.internalError("Update pipeline completion failed", error);
                }
                if (pipelineResult == null || !pipelineResult.isAccepted()) {
                    log.debug("Webhook pipeline rejected updateId={}", update.updateId().value());
                    return WebhookReceiveResult.internalError(
                            "Update pipeline reported handling failure",
                            pipelineResult == null ? null : pipelineResult.error().orElse(null)
                    );
                }
                log.debug("Webhook update accepted: updateId={}", update.updateId().value());
                return WebhookReceiveResult.accepted(validationResult);
            }).whenComplete((ignored, ignoredError) -> inFlight.release());
        } catch (RuntimeException runtimeException) {
            inFlight.release();
            log.debug("Webhook receiver internal error", runtimeException);
            return CompletableFuture.completedFuture(
                    WebhookReceiveResult.internalError("Unexpected webhook receiver failure", runtimeException)
            );
        }
    }

    private CompletionStage<WebhookReceiveResult> completed(WebhookReceiveResult result) {
        inFlight.release();
        return CompletableFuture.completedFuture(result);
    }

    private static UpdateSink wrap(UpdateConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return consumer::handle;
    }

    private static boolean isMeaningfulApiUpdate(ApiUpdate update) {
        return update != null
                && (update.updateType() != null
                || update.updateId() != null
                || hasMeaningfulApiMessage(update.message())
                || hasMeaningfulApiCallback(update.callback()));
    }

    private static boolean hasMeaningfulApiMessage(ru.tardyon.botframework.model.transport.ApiMessage message) {
        return message != null
                && (message.messageId() != null
                || message.sender() != null
                || message.recipient() != null
                || message.timestamp() != null
                || message.body() != null
                || message.link() != null);
    }

    private static boolean hasMeaningfulApiCallback(ru.tardyon.botframework.model.transport.ApiCallback callback) {
        return callback != null
                && (callback.callbackId() != null
                || callback.timestamp() != null
                || callback.sender() != null
                || callback.message() != null
                || callback.data() != null);
    }
}
