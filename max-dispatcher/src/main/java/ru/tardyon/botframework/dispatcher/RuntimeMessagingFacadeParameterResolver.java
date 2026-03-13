package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.message.MessagingFacade;

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
