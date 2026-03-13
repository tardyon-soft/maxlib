package ru.max.botframework.dispatcher;

import java.util.Objects;
import ru.max.botframework.fsm.WizardManager;

/**
 * Resolves {@link WizardManager} for handlers when scene runtime is configured.
 */
public final class WizardManagerParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");

        if (!parameter.type().isAssignableFrom(WizardManager.class)) {
            return HandlerParameterResolution.unsupported();
        }
        return SceneRuntimeSupport.resolveWizardManager(context.runtimeContext())
                .<HandlerParameterResolution>map(HandlerParameterResolution::resolved)
                .orElseGet(HandlerParameterResolution::unsupported);
    }
}
