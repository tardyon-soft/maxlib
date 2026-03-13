package ru.tardyon.botframework.dispatcher;

import java.util.Objects;

/**
 * Resolves {@link RuntimeContext} parameter.
 */
public final class RuntimeContextParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        return parameter.type().isAssignableFrom(RuntimeContext.class)
                ? HandlerParameterResolution.resolved(context.runtimeContext())
                : HandlerParameterResolution.unsupported();
    }
}
