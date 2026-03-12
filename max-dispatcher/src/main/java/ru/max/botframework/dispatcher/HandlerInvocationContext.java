package ru.max.botframework.dispatcher;

import java.util.Objects;
import ru.max.botframework.model.Update;

/**
 * Request-scoped invocation context used by handler invoker and parameter resolvers.
 */
public record HandlerInvocationContext(Object event, RuntimeContext runtimeContext) {
    public HandlerInvocationContext {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(runtimeContext, "runtimeContext");
    }

    public Update update() {
        return runtimeContext.update();
    }
}
