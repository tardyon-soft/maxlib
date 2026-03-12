package ru.max.botframework.dispatcher;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Signals that no resolver can provide value for required handler parameter.
 */
public final class UnsupportedHandlerParameterException extends ParameterResolutionException {
    private final Method method;
    private final HandlerParameterDescriptor parameter;

    UnsupportedHandlerParameterException(Method method, HandlerParameterDescriptor parameter) {
        super(
                Reason.UNSUPPORTED_PARAMETER,
                "unsupported handler parameter '%s' (%s) for method %s"
                        .formatted(parameter.name(), parameter.type().getSimpleName(), method.toGenericString()),
                method,
                parameter,
                null
        );
        this.method = Objects.requireNonNull(method, "method");
        this.parameter = Objects.requireNonNull(parameter, "parameter");
    }

    Method method() {
        return method;
    }

    HandlerParameterDescriptor parameter() {
        return parameter;
    }
}
