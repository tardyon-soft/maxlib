package ru.tardyon.botframework.dispatcher;

import java.util.concurrent.CompletionStage;

/**
 * Proceed contract for middleware chains.
 */
@FunctionalInterface
public interface MiddlewareNext {
    /**
     * Continues middleware chain and returns downstream dispatch result.
     */
    CompletionStage<DispatchResult> proceed();
}
