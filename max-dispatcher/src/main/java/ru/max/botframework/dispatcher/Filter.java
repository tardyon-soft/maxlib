package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Transport-agnostic filter contract for runtime event matching.
 *
 * @param <TEvent> event type
 */
@FunctionalInterface
public interface Filter<TEvent> {

    CompletionStage<FilterResult> test(TEvent event);

    static <TEvent> Filter<TEvent> any() {
        return event -> CompletableFuture.completedFuture(FilterResult.matched());
    }

    static <TEvent> Filter<TEvent> of(Predicate<TEvent> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return event -> {
            try {
                return CompletableFuture.completedFuture(predicate.test(event)
                        ? FilterResult.matched()
                        : FilterResult.notMatched());
            } catch (Throwable throwable) {
                return CompletableFuture.completedFuture(FilterResult.failed(throwable));
            }
        };
    }
}

