package ru.tardyon.botframework.dispatcher;

import java.lang.reflect.Method;

/**
 * Signals that handler requires dependency/data which is not available in runtime resolution sources.
 */
public final class MissingHandlerDependencyException extends ParameterResolutionException {

    MissingHandlerDependencyException(Method method, HandlerParameterDescriptor parameter) {
        super(
                Reason.MISSING_DEPENDENCY,
                "missing required dependency/data for parameter '%s' (%s) in method %s"
                        .formatted(parameter.name(), parameter.type().getSimpleName(), method.toGenericString()),
                method,
                parameter,
                null
        );
    }
}
