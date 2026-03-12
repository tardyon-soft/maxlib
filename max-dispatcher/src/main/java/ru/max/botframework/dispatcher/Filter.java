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

    default Filter<TEvent> and(Filter<? super TEvent> other) {
        Objects.requireNonNull(other, "other");
        return event -> test(event).thenCompose(left -> {
            if (left.status() == FilterStatus.FAILED || left.status() == FilterStatus.NOT_MATCHED) {
                return CompletableFuture.completedFuture(left);
            }
            return other.test(event).thenApply(right -> {
                if (right.status() == FilterStatus.FAILED || right.status() == FilterStatus.NOT_MATCHED) {
                    return right;
                }
                return left.mergeMatched(right);
            });
        });
    }

    default Filter<TEvent> or(Filter<? super TEvent> other) {
        Objects.requireNonNull(other, "other");
        return event -> test(event).thenCompose(left -> {
            if (left.status() == FilterStatus.FAILED || left.status() == FilterStatus.MATCHED) {
                return CompletableFuture.completedFuture(left);
            }
            return other.test(event);
        });
    }

    default Filter<TEvent> not() {
        return event -> test(event).thenApply(result -> {
            if (result.status() == FilterStatus.FAILED) {
                return result;
            }
            return result.status() == FilterStatus.MATCHED
                    ? FilterResult.notMatched()
                    : FilterResult.matched();
        });
    }

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
