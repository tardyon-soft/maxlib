package ru.tardyon.botframework.dispatcher;

import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Reflective descriptor for one handler parameter.
 *
 * <p>This descriptor is immutable and may be cached by invocation engine.</p>
 *
 * @param index parameter index in declaration order
 * @param parameter reflective parameter
 */
public record HandlerParameterDescriptor(int index, Parameter parameter) {
    public HandlerParameterDescriptor {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        Objects.requireNonNull(parameter, "parameter");
    }

    public String name() {
        return parameter.isNamePresent() ? parameter.getName() : "arg" + index;
    }

    public Class<?> type() {
        return parameter.getType();
    }
}
