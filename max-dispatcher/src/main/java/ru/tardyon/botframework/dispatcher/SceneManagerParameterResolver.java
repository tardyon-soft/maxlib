package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import ru.tardyon.botframework.fsm.SceneManager;

/**
 * Resolves {@link SceneManager} for handlers when scene runtime is configured.
 */
public final class SceneManagerParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");

        if (!parameter.type().isAssignableFrom(SceneManager.class)) {
            return HandlerParameterResolution.unsupported();
        }
        return SceneRuntimeSupport.resolveSceneManager(context.runtimeContext())
                .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                .orElseGet(HandlerParameterResolution::unsupported);
    }
}
