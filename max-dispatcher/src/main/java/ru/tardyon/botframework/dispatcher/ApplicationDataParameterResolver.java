package ru.tardyon.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves application-scoped runtime data by parameter type.
 */
public final class ApplicationDataParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");

        Map<String, Object> appData = context.runtimeContext().data().snapshot(RuntimeDataScope.APPLICATION);
        Object found = null;
        String foundKey = null;
        for (Map.Entry<String, Object> entry : appData.entrySet()) {
            Object candidate = entry.getValue();
            if (!parameter.type().isInstance(candidate)) {
                continue;
            }
            if (found != null) {
                throw new IllegalStateException(
                        "ambiguous application data for parameter '%s' (%s): keys '%s' and '%s'"
                                .formatted(parameter.name(), parameter.type().getSimpleName(), foundKey, entry.getKey())
                );
            }
            found = candidate;
            foundKey = entry.getKey();
        }
        return found == null
                ? HandlerParameterResolution.unsupported()
                : HandlerParameterResolution.resolved(found);
    }
}
