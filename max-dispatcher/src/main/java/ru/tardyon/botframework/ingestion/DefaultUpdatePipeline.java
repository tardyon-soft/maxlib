package ru.tardyon.botframework.ingestion;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.model.Update;

/**
 * Default unified ingestion pipeline delegating normalized updates to sink.
 */
public final class DefaultUpdatePipeline implements UpdatePipeline {
    private final UpdateSink sink;
    private final List<UpdatePipelineHook> hooks;

    public DefaultUpdatePipeline(UpdateSink sink) {
        this(sink, List.of());
    }

    public DefaultUpdatePipeline(UpdateSink sink, List<UpdatePipelineHook> hooks) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.hooks = hooks == null ? List.of() : List.copyOf(hooks);
    }

    @Override
    public CompletionStage<UpdatePipelineResult> process(Update update, UpdatePipelineContext context) {
        Update normalizedUpdate = normalize(update);
        UpdatePipelineContext normalizedContext = Objects.requireNonNull(context, "context");
        hooks.forEach(hook -> hook.onBefore(normalizedUpdate, normalizedContext));

        CompletionStage<UpdateHandlingResult> sinkStage;
        try {
            sinkStage = sink.handle(normalizedUpdate);
        } catch (RuntimeException runtimeException) {
            UpdatePipelineResult result = UpdatePipelineResult.rejected(runtimeException);
            hooks.forEach(hook -> hook.onAfter(normalizedUpdate, normalizedContext, result));
            return java.util.concurrent.CompletableFuture.completedFuture(result);
        }

        return sinkStage.handle((handlingResult, error) -> {
            UpdatePipelineResult result;
            if (error != null) {
                result = UpdatePipelineResult.rejected(unwrap(error));
            } else if (handlingResult == null || !handlingResult.isSuccess()) {
                result = UpdatePipelineResult.rejected(handlingResult == null ? null : handlingResult.error().orElse(null));
            } else {
                result = UpdatePipelineResult.accepted();
            }
            hooks.forEach(hook -> hook.onAfter(normalizedUpdate, normalizedContext, result));
            return result;
        });
    }

    private static Update normalize(Update update) {
        Objects.requireNonNull(update, "update");
        Objects.requireNonNull(update.updateId(), "update.updateId");
        Objects.requireNonNull(update.type(), "update.type");
        return update;
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }
}
