package ru.max.botframework.dispatcher;

import java.util.Objects;

/**
 * Resolves current event object if parameter type is compatible.
 */
public final class EventParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        Object event = context.event();
        return parameter.type().isInstance(event)
                ? HandlerParameterResolution.resolved(event)
                : HandlerParameterResolution.unsupported();
    }
}
