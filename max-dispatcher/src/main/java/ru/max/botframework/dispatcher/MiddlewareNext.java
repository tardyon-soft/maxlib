package ru.max.botframework.dispatcher;

import java.util.concurrent.CompletionStage;

/**
 * Proceed contract for middleware chains.
 */
@FunctionalInterface
public interface MiddlewareNext {
    CompletionStage<DispatchResult> proceed();
}

