package ru.tardyon.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves filter-produced runtime data by unique parameter type.
 */
public final class FilterDataParameterResolver implements HandlerParameterResolver {
    @Override
    public HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        return resolveByType(parameter, context.runtimeContext().data().snapshot(RuntimeDataScope.FILTER), "filter");
    }

    static HandlerParameterResolution resolveByType(
            HandlerParameterDescriptor parameter,
            Map<String, Object> data,
            String sourceName
    ) {
        Object found = null;
        String foundKey = null;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object candidate = entry.getValue();
            if (!parameter.type().isInstance(candidate)) {
                continue;
            }
            if (found != null) {
                throw new IllegalStateException(
                        "ambiguous %s data for parameter '%s' (%s): keys '%s' and '%s'"
                                .formatted(
                                        sourceName,
                                        parameter.name(),
                                        parameter.type().getSimpleName(),
                                        foundKey,
                                        entry.getKey()
                                )
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
