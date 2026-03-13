package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import ru.tardyon.botframework.model.Update;

/**
 * Resolves current update object.
 */
public final class UpdateParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        Update update = context.update();
        return parameter.type().isInstance(update)
                ? HandlerParameterResolution.resolved(update)
                : HandlerParameterResolution.unsupported();
    }
}
