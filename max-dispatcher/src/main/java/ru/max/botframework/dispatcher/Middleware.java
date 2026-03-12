package ru.max.botframework.dispatcher;

import java.util.concurrent.CompletionStage;

/**
 * Middleware contract for pre/post processing around dispatch pipeline stages.
 */
@FunctionalInterface
public interface Middleware {
    /**
     * Invokes middleware with request-scoped context and next link in chain.
     *
     * <p>Returning result without calling {@code next.proceed()} short-circuits chain execution.</p>
     */
    CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next);
}
