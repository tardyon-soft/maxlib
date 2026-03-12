package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.Update;

/**
 * Resolves message payload from message/callback/update events.
 */
public final class MessageParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        if (parameter.type() != Message.class) {
            return HandlerParameterResolution.unsupported();
        }
        return extractMessage(context.event(), context.update())
                .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                .orElseGet(HandlerParameterResolution::unsupported);
    }

    private static Optional<Message> extractMessage(Object event, Update update) {
        if (event instanceof Message message) {
            return Optional.of(message);
        }
        if (event instanceof Callback callback) {
            return Optional.ofNullable(callback.message());
        }
        if (event instanceof Update eventUpdate) {
            if (eventUpdate.message() != null) {
                return Optional.of(eventUpdate.message());
            }
            if (eventUpdate.callback() != null) {
                return Optional.ofNullable(eventUpdate.callback().message());
            }
        }
        if (update.message() != null) {
            return Optional.of(update.message());
        }
        if (update.callback() != null) {
            return Optional.ofNullable(update.callback().message());
        }
        return Optional.empty();
    }
}
