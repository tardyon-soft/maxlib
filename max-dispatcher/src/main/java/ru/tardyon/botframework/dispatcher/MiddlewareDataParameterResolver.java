package ru.tardyon.botframework.dispatcher;

import java.util.Objects;

/**
 * Resolves middleware-produced runtime data by unique parameter type.
 */
public final class MiddlewareDataParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        return FilterDataParameterResolver.resolveByType(
                parameter,
                context.runtimeContext().data().snapshot(RuntimeDataScope.MIDDLEWARE),
                "middleware"
        );
    }
}
