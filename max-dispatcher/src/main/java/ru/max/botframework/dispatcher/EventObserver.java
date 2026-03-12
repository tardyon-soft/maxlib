package ru.max.botframework.dispatcher;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Observer for one event kind with handler registration and dispatch participation.
 *
 * @param <TEvent> observed event type
 */
public interface EventObserver<TEvent> {

    /**
     * Observer kind this registry is bound to.
     */
    ObserverType type();

    /**
     * Registers handler in insertion order.
     */
    EventObserver<TEvent> register(EventHandler<TEvent> handler);

    /**
     * Registers handler with filter in insertion order.
     */
    EventObserver<TEvent> register(Filter<TEvent> filter, EventHandler<TEvent> handler);

    /**
     * Registered handlers in immutable registration order snapshot.
     */
    List<EventHandler<TEvent>> handlers();

    /**
     * Notifies observer about one event and returns handler execution outcome.
     */
    CompletionStage<HandlerExecutionResult> notify(TEvent event);

    /**
     * Notifies observer about one event using custom matched-handler execution strategy.
     *
     * <p>Framework-internal extension point used by dispatcher pipeline to wrap handler execution
     * with runtime phases (for example inner middleware). Application code should prefer
     * {@link #notify(Object)}.</p>
     */
    CompletionStage<HandlerExecutionResult> notify(TEvent event, HandlerExecutionStrategy<TEvent> strategy);
}
