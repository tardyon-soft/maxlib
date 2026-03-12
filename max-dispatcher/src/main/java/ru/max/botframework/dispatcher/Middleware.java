package ru.max.botframework.dispatcher;

import java.util.concurrent.CompletionStage;

/**
 * Middleware contract for pre/post processing around dispatch pipeline stages.
 */
@FunctionalInterface
public interface Middleware {
    CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next);
}

