package ru.tardyon.botframework.dispatcher;

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

    /**
     * Evaluates filter against one event.
     */
    CompletionStage<FilterResult> test(TEvent event);

    /**
     * Evaluates filter with optional runtime context.
     *
     * <p>Default implementation preserves backward compatibility and delegates to
     * {@link #test(Object)}.</p>
     */
    default CompletionStage<FilterResult> test(TEvent event, RuntimeContext context) {
        return test(event);
    }

    /**
     * Logical AND composition with short-circuit semantics.
     */
    default Filter<TEvent> and(Filter<? super TEvent> other) {
        Objects.requireNonNull(other, "other");
        return new Filter<>() {
            @Override
            public CompletionStage<FilterResult> test(TEvent event) {
                return evaluate(event, null);
            }

            @Override
            public CompletionStage<FilterResult> test(TEvent event, RuntimeContext context) {
                return evaluate(event, context);
            }

            private CompletionStage<FilterResult> evaluate(TEvent event, RuntimeContext context) {
                return Filter.this.test(event, context).thenCompose(left -> {
                    if (left.status() == FilterStatus.FAILED || left.status() == FilterStatus.NOT_MATCHED) {
                        return CompletableFuture.completedFuture(left);
                    }
                    return other.test(event, context).thenApply(right -> {
                        if (right.status() == FilterStatus.FAILED || right.status() == FilterStatus.NOT_MATCHED) {
                            return right;
                        }
                        return left.mergeMatched(right);
                    });
                });
            }
        };
    }

    /**
     * Logical OR composition with short-circuit semantics.
     */
    default Filter<TEvent> or(Filter<? super TEvent> other) {
        Objects.requireNonNull(other, "other");
        return new Filter<>() {
            @Override
            public CompletionStage<FilterResult> test(TEvent event) {
                return evaluate(event, null);
            }

            @Override
            public CompletionStage<FilterResult> test(TEvent event, RuntimeContext context) {
                return evaluate(event, context);
            }

            private CompletionStage<FilterResult> evaluate(TEvent event, RuntimeContext context) {
                return Filter.this.test(event, context).thenCompose(left -> {
                    if (left.status() == FilterStatus.FAILED || left.status() == FilterStatus.MATCHED) {
                        return CompletableFuture.completedFuture(left);
                    }
                    return other.test(event, context);
                });
            }
        };
    }

    /**
     * Logical NOT composition. Enrichment from source filter is never propagated.
     */
    default Filter<TEvent> not() {
        return new Filter<>() {
            @Override
            public CompletionStage<FilterResult> test(TEvent event) {
                return evaluate(event, null);
            }

            @Override
            public CompletionStage<FilterResult> test(TEvent event, RuntimeContext context) {
                return evaluate(event, context);
            }

            private CompletionStage<FilterResult> evaluate(TEvent event, RuntimeContext context) {
                return Filter.this.test(event, context).thenApply(result -> {
                    if (result.status() == FilterStatus.FAILED) {
                        return result;
                    }
                    return result.status() == FilterStatus.MATCHED
                            ? FilterResult.notMatched()
                            : FilterResult.matched();
                });
            }
        };
    }

    /**
     * Filter that always returns {@link FilterStatus#MATCHED}.
     */
    static <TEvent> Filter<TEvent> any() {
        return event -> CompletableFuture.completedFuture(FilterResult.matched());
    }

    /**
     * Adapts synchronous predicate to {@link Filter}.
     */
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
