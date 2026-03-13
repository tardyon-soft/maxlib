package ru.tardyon.botframework.dispatcher;

import java.util.concurrent.CompletionStage;

/**
 * Async-capable event handler contract used by event observers.
 *
 * <p>For handlers that need runtime context access use {@link ContextualEventHandler}.</p>
 *
 * @param <TEvent> event payload type
 */
@FunctionalInterface
public interface EventHandler<TEvent> {
    CompletionStage<Void> handle(TEvent event);
}
