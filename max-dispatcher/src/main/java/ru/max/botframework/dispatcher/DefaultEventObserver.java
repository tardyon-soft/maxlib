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
    private final CopyOnWriteArrayList<Registration<TEvent>> registrations;

    public DefaultEventObserver(ObserverType type) {
        this.type = Objects.requireNonNull(type, "type");
        this.registrations = new CopyOnWriteArrayList<>();
    }

    @Override
    public ObserverType type() {
        return type;
    }

    @Override
    public EventObserver<TEvent> register(EventHandler<TEvent> handler) {
        return register(Filter.any(), handler);
    }

    @Override
    public EventObserver<TEvent> register(Filter<TEvent> filter, EventHandler<TEvent> handler) {
        registrations.add(new Registration<>(
                Objects.requireNonNull(filter, "filter"),
                Objects.requireNonNull(handler, "handler")
        ));
        return this;
    }

    @Override
    public List<EventHandler<TEvent>> handlers() {
        return registrations.stream().map(Registration::handler).toList();
    }

    @Override
    public CompletionStage<HandlerExecutionResult> notify(TEvent event) {
        return notify(event, HandlerExecutionStrategy.direct());
    }

    @Override
    public CompletionStage<HandlerExecutionResult> notify(TEvent event, HandlerExecutionStrategy<TEvent> strategy) {
        Objects.requireNonNull(strategy, "strategy");
        if (registrations.isEmpty()) {
            return CompletableFuture.completedFuture(HandlerExecutionResult.ignored());
        }

        return notifyFromRegistration(event, 0, strategy);
    }

    private CompletionStage<HandlerExecutionResult> notifyFromRegistration(
            TEvent event,
            int index,
            HandlerExecutionStrategy<TEvent> strategy
    ) {
        if (index >= registrations.size()) {
            return CompletableFuture.completedFuture(HandlerExecutionResult.ignored());
        }
        Registration<TEvent> registration = registrations.get(index);
        CompletionStage<FilterResult> filterStage;
        try {
            filterStage = Objects.requireNonNull(registration.filter().test(event), "filter result");
        } catch (Throwable throwable) {
            return CompletableFuture.completedFuture(HandlerExecutionResult.failed(throwable));
        }
        return filterStage.handle((result, throwable) -> new FilterEvaluationOutcome(result, throwable))
                .thenCompose(outcome -> {
                    if (outcome.throwable() != null) {
                        return CompletableFuture.completedFuture(
                                HandlerExecutionResult.failed(unwrap(outcome.throwable()))
                        );
                    }
                    FilterResult result = outcome.result();
                    if (result.status() == FilterStatus.FAILED) {
                        return CompletableFuture.completedFuture(
                                HandlerExecutionResult.failed(result.errorOpt().orElseGet(() -> new IllegalStateException("filter failed")))
                        );
                    }
                    if (result.status() == FilterStatus.NOT_MATCHED) {
                        return notifyFromRegistration(event, index + 1, strategy);
                    }
                    return invokeHandler(registration.handler(), event, result.enrichment(), strategy);
                });
    }

    private CompletionStage<HandlerExecutionResult> invokeHandler(
            EventHandler<TEvent> handler,
            TEvent event,
            java.util.Map<String, Object> enrichment,
            HandlerExecutionStrategy<TEvent> strategy
    ) {
        try {
            return Objects.requireNonNull(strategy.execute(handler, event, enrichment), "handler execution strategy result")
                    .handle((result, throwable) -> throwable == null
                            ? Objects.requireNonNull(result, "handler execution result")
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

    private record Registration<TEvent>(Filter<TEvent> filter, EventHandler<TEvent> handler) {
    }

    private record FilterEvaluationOutcome(FilterResult result, Throwable throwable) {
    }
}
