package ru.max.botframework.dispatcher;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default first-match observer implementation for Sprint 3 foundation.
 *
 * @param <TEvent> event type
 */
public final class DefaultEventObserver<TEvent> implements EventObserver<TEvent> {
    private final ObserverType type;
    private final CopyOnWriteArrayList<EventHandler<TEvent>> handlers;

    public DefaultEventObserver(ObserverType type) {
        this.type = Objects.requireNonNull(type, "type");
        this.handlers = new CopyOnWriteArrayList<>();
    }

    @Override
    public ObserverType type() {
        return type;
    }

    @Override
    public EventObserver<TEvent> register(EventHandler<TEvent> handler) {
        handlers.add(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    @Override
    public List<EventHandler<TEvent>> handlers() {
        return List.copyOf(handlers);
    }

    @Override
    public CompletionStage<HandlerExecutionResult> notify(TEvent event) {
        if (handlers.isEmpty()) {
            return CompletableFuture.completedFuture(HandlerExecutionResult.ignored());
        }

        EventHandler<TEvent> handler = handlers.getFirst();
        try {
            CompletionStage<Void> stage = Objects.requireNonNull(handler.handle(event), "handler result");
            return stage.handle((ignored, throwable) -> throwable == null
                    ? HandlerExecutionResult.handled()
                    : HandlerExecutionResult.failed(unwrap(throwable)));
        } catch (Throwable throwable) {
            return CompletableFuture.completedFuture(HandlerExecutionResult.failed(throwable));
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }
}

