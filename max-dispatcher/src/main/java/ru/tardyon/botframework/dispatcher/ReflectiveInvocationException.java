package ru.tardyon.botframework.dispatcher;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Signals reflection infrastructure failure while invoking method-based handler.
 */
public final class ReflectiveInvocationException extends IllegalStateException {
    private final Method method;

    private ReflectiveInvocationException(String message, Method method, Throwable cause) {
        super(message, cause);
        this.method = Objects.requireNonNull(method, "method");
    }

    public Method method() {
        return method;
    }

    static ReflectiveInvocationException invocationFailure(Method method, Throwable cause) {
        return new ReflectiveInvocationException(
                "reflective invocation failed for method %s".formatted(method.toGenericString()),
                method,
                cause
        );
    }

    static ReflectiveInvocationException invalidReturnType(Method method, Object value) {
        String type = value == null ? "null" : value.getClass().getName();
        return new ReflectiveInvocationException(
                "handler method must return void or CompletionStage, but got %s in %s"
                        .formatted(type, method.toGenericString()),
                method,
                null
        );
    }
}
