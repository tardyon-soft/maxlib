package ru.max.botframework.dispatcher;

import java.util.concurrent.CompletionStage;

/**
 * Async-capable event handler contract used by event observers.
 *
 * @param <TEvent> event payload type
 */
@FunctionalInterface
public interface EventHandler<TEvent> {
    CompletionStage<Void> handle(TEvent event);
}

