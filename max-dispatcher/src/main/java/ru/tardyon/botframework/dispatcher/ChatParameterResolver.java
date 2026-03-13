package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.Update;

/**
 * Resolves chat from event/update payload.
 */
public final class ChatParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        if (parameter.type() != Chat.class) {
            return HandlerParameterResolution.unsupported();
        }
        return extractChat(context.event(), context.update())
                .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                .orElseGet(HandlerParameterResolution::unsupported);
    }

    private static Optional<Chat> extractChat(Object event, Update update) {
        if (event instanceof Chat chat) {
            return Optional.of(chat);
        }
        if (event instanceof Message message) {
            return Optional.ofNullable(message.chat());
        }
        if (event instanceof Callback callback) {
            return callback.message() == null ? Optional.empty() : Optional.ofNullable(callback.message().chat());
        }
        if (event instanceof Update eventUpdate) {
            return fromUpdate(eventUpdate);
        }
        return fromUpdate(update);
    }

    private static Optional<Chat> fromUpdate(Update update) {
        if (update.message() != null) {
            return Optional.ofNullable(update.message().chat());
        }
        if (update.callback() != null && update.callback().message() != null) {
            return Optional.ofNullable(update.callback().message().chat());
        }
        return Optional.empty();
    }
}
