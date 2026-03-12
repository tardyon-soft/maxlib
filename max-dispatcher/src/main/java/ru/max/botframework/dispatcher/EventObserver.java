package ru.max.botframework.dispatcher;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Observer for one event kind with handler registration and dispatch participation.
 *
 * @param <TEvent> observed event type
 */
public interface EventObserver<TEvent> {

    ObserverType type();

    EventObserver<TEvent> register(EventHandler<TEvent> handler);

    List<EventHandler<TEvent>> handlers();

    CompletionStage<HandlerExecutionResult> notify(TEvent event);
}

