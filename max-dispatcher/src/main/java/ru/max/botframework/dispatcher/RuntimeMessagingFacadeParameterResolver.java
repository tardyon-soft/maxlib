package ru.max.botframework.dispatcher;

import java.util.Objects;
import ru.max.botframework.action.ChatActionsFacade;
import ru.max.botframework.callback.CallbackFacade;
import ru.max.botframework.message.MediaMessagingFacade;
import ru.max.botframework.message.MessagingFacade;

/**
 * Resolves high-level runtime messaging facades injected into runtime context.
 */
public final class RuntimeMessagingFacadeParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");

        if (parameter.type() == MessagingFacade.class) {
            return context.runtimeContext().dataValue(RuntimeMessagingSupport.MESSAGING_FACADE_KEY)
                    .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                    .orElseGet(HandlerParameterResolution::unsupported);
        }
        if (parameter.type() == CallbackFacade.class) {
            return context.runtimeContext().dataValue(RuntimeMessagingSupport.CALLBACK_FACADE_KEY)
                    .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                    .orElseGet(HandlerParameterResolution::unsupported);
        }
        if (parameter.type() == ChatActionsFacade.class) {
            return context.runtimeContext().dataValue(RuntimeMessagingSupport.CHAT_ACTIONS_FACADE_KEY)
                    .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                    .orElseGet(HandlerParameterResolution::unsupported);
        }
        if (parameter.type() == MediaMessagingFacade.class) {
            return context.runtimeContext().dataValue(RuntimeMessagingSupport.MEDIA_MESSAGING_FACADE_KEY)
                    .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                    .orElseGet(HandlerParameterResolution::unsupported);
        }
        return HandlerParameterResolution.unsupported();
    }
}
