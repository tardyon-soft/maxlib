package ru.tardyon.botframework.dispatcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Java-friendly handler contract with explicit access to runtime context.
 *
 * <p>This is the primary Sprint 5 signature model for future parameter resolution growth.
 * It keeps event typing explicit and avoids annotation-heavy magic.</p>
 *
 * @param <TEvent> event payload type
 */
@FunctionalInterface
public interface ContextualEventHandler<TEvent> extends EventHandler<TEvent> {

    CompletionStage<Void> handle(TEvent event, RuntimeContext context);

    @Override
    default CompletionStage<Void> handle(TEvent event) {
        return CompletableFuture.failedFuture(new IllegalStateException(
                "ContextualEventHandler requires RuntimeContext-aware invocation"
        ));
    }
}
