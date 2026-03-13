package ru.tardyon.botframework.dispatcher;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Utility for executing middleware chains with proceed model.
 */
public final class MiddlewareChainExecutor {

    private MiddlewareChainExecutor() {
    }

    public static CompletionStage<DispatchResult> executeOuter(
            RuntimeContext context,
            List<OuterMiddleware> chain,
            Supplier<CompletionStage<DispatchResult>> terminal
    ) {
        return execute(context, List.copyOf(chain), terminal, MiddlewareExecutionException.Phase.OUTER);
    }

    public static CompletionStage<DispatchResult> executeInner(
            RuntimeContext context,
            List<InnerMiddleware> chain,
            Supplier<CompletionStage<DispatchResult>> terminal
    ) {
        return execute(context, List.copyOf(chain), terminal, MiddlewareExecutionException.Phase.INNER);
    }

    private static CompletionStage<DispatchResult> execute(
            RuntimeContext context,
            List<? extends Middleware> chain,
            Supplier<CompletionStage<DispatchResult>> terminal,
            MiddlewareExecutionException.Phase phase
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(chain, "chain");
        Objects.requireNonNull(terminal, "terminal");
        CompletableFuture<DispatchResult> result = new CompletableFuture<>();
        proceed(context, chain, 0, terminal)
                .whenComplete((dispatchResult, throwable) -> {
                    if (throwable != null) {
                        result.completeExceptionally(MiddlewareExecutionException.wrap(phase, unwrap(throwable)));
                    } else {
                        result.complete(dispatchResult);
                    }
                });
        return result;
    }

    private static CompletionStage<DispatchResult> proceed(
            RuntimeContext context,
            List<? extends Middleware> chain,
            int index,
            Supplier<CompletionStage<DispatchResult>> terminal
    ) {
        if (index >= chain.size()) {
            return Objects.requireNonNull(terminal.get(), "terminal result");
        }
        Middleware middleware = Objects.requireNonNull(chain.get(index), "middleware");
        return middleware.invoke(context, () -> proceed(context, chain, index + 1, terminal))
                .thenCompose(result -> CompletableFuture.completedFuture(
                        Objects.requireNonNull(result, "middleware result")
                ));
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof java.util.concurrent.CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
    }
}
