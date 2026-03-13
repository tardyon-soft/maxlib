package ru.max.botframework.dispatcher;

import java.util.Objects;
import ru.max.botframework.fsm.FSMContext;

/**
 * Resolves {@link FSMContext} for handlers when FSM runtime is configured on dispatcher.
 */
public final class FSMContextParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");

        if (!parameter.type().isAssignableFrom(FSMContext.class)) {
            return HandlerParameterResolution.unsupported();
        }

        return FSMRuntimeSupport.resolve(context.runtimeContext())
                .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                .orElseGet(HandlerParameterResolution::unsupported);
    }
}
