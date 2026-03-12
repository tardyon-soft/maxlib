package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.Update;

/**
 * Resolves callback payload from callback/update events.
 */
public final class CallbackParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        if (parameter.type() != Callback.class) {
            return HandlerParameterResolution.unsupported();
        }
        return extractCallback(context.event(), context.update())
                .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                .orElseGet(HandlerParameterResolution::unsupported);
    }

    private static Optional<Callback> extractCallback(Object event, Update update) {
        if (event instanceof Callback callback) {
            return Optional.of(callback);
        }
        if (event instanceof Update eventUpdate) {
            return Optional.ofNullable(eventUpdate.callback());
        }
        return Optional.ofNullable(update.callback());
    }
}
