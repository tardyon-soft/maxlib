package ru.max.botframework.dispatcher;

import java.util.Objects;

/**
 * One resolver attempt result for one handler parameter.
 */
public record HandlerParameterResolution(boolean supported, Object value) {

    public HandlerParameterResolution {
        if (supported && value == null) {
            throw new IllegalArgumentException("resolved value must not be null");
        }
    }

    public static HandlerParameterResolution unsupported() {
        return new HandlerParameterResolution(false, null);
    }

    public static HandlerParameterResolution resolved(Object value) {
        return new HandlerParameterResolution(true, Objects.requireNonNull(value, "value"));
    }
}
