package ru.max.botframework.dispatcher;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Signals failure during handler parameter resolution.
 */
public class ParameterResolutionException extends IllegalStateException {
    private final Reason reason;
    private final Method method;
    private final HandlerParameterDescriptor parameter;

    ParameterResolutionException(
            Reason reason,
            String message,
            Method method,
            HandlerParameterDescriptor parameter,
            Throwable cause
    ) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason");
        this.method = Objects.requireNonNull(method, "method");
        this.parameter = Objects.requireNonNull(parameter, "parameter");
    }

    public Reason reason() {
        return reason;
    }

    Method method() {
        return method;
    }

    HandlerParameterDescriptor parameter() {
        return parameter;
    }

    static ParameterResolutionException ambiguous(
            Method method,
            HandlerParameterDescriptor parameter,
            String sourceName,
            String firstKey,
            String secondKey
    ) {
        return new ParameterResolutionException(
                Reason.AMBIGUOUS_RESOLUTION,
                "ambiguous %s data for parameter '%s' (%s): keys '%s' and '%s' in %s"
                        .formatted(
                                sourceName,
                                parameter.name(),
                                parameter.type().getSimpleName(),
                                firstKey,
                                secondKey,
                                method.toGenericString()
                        ),
                method,
                parameter,
                null
        );
    }

    static ParameterResolutionException ambiguous(
            Method method,
            HandlerParameterDescriptor parameter,
            String resolverName,
            Throwable cause
    ) {
        return new ParameterResolutionException(
                Reason.AMBIGUOUS_RESOLUTION,
                "ambiguous parameter resolution for '%s' (%s) in %s via resolver %s"
                        .formatted(
                                parameter.name(),
                                parameter.type().getSimpleName(),
                                method.toGenericString(),
                                resolverName
                        ),
                method,
                parameter,
                cause
        );
    }

    static ParameterResolutionException resolverFailure(
            Method method,
            HandlerParameterDescriptor parameter,
            String resolverName,
            Throwable cause
    ) {
        return new ParameterResolutionException(
                Reason.RESOLVER_FAILURE,
                "resolver %s failed to resolve parameter '%s' (%s) for method %s"
                        .formatted(
                                resolverName,
                                parameter.name(),
                                parameter.type().getSimpleName(),
                                method.toGenericString()
                        ),
                method,
                parameter,
                cause
        );
    }

    public enum Reason {
        UNSUPPORTED_PARAMETER,
        MISSING_DEPENDENCY,
        AMBIGUOUS_RESOLUTION,
        RESOLVER_FAILURE
    }
}
