package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.User;

/**
 * Resolves actor user from event/update payload.
 */
public final class UserParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        if (parameter.type() != User.class) {
            return HandlerParameterResolution.unsupported();
        }
        return extractUser(context.event(), context.update())
                .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                .orElseGet(HandlerParameterResolution::unsupported);
    }

    private static Optional<User> extractUser(Object event, Update update) {
        if (event instanceof User user) {
            return Optional.of(user);
        }
        if (event instanceof Message message) {
            return Optional.ofNullable(message.from());
        }
        if (event instanceof Callback callback) {
            if (callback.from() != null) {
                return Optional.of(callback.from());
            }
            return callback.message() == null ? Optional.empty() : Optional.ofNullable(callback.message().from());
        }
        if (event instanceof Update eventUpdate) {
            return fromUpdate(eventUpdate);
        }
        return fromUpdate(update);
    }

    private static Optional<User> fromUpdate(Update update) {
        if (update.message() != null && update.message().from() != null) {
            return Optional.of(update.message().from());
        }
        if (update.callback() != null) {
            if (update.callback().from() != null) {
                return Optional.of(update.callback().from());
            }
            if (update.callback().message() != null) {
                return Optional.ofNullable(update.callback().message().from());
            }
        }
        return Optional.empty();
    }
}
