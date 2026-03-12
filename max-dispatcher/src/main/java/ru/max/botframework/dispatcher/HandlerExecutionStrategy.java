package ru.max.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Strategy hook for executing matched handler after filter evaluation.
 *
 * @param <TEvent> event type
 */
@FunctionalInterface
public interface HandlerExecutionStrategy<TEvent> {

    CompletionStage<HandlerExecutionResult> execute(
            EventHandler<TEvent> handler,
            TEvent event,
            Map<String, Object> enrichment
    );

    static <TEvent> HandlerExecutionStrategy<TEvent> direct() {
        return (handler, event, enrichment) -> {
            try {
                CompletionStage<Void> stage = Objects.requireNonNull(handler.handle(event), "handler result");
                return stage.handle((ignored, throwable) -> throwable == null
                        ? HandlerExecutionResult.handled(enrichment)
                        : HandlerExecutionResult.failed(throwable));
            } catch (Throwable throwable) {
                return CompletableFuture.completedFuture(HandlerExecutionResult.failed(throwable));
            }
        };
    }
}

