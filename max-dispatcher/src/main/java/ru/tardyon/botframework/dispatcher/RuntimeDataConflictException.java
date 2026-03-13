package ru.tardyon.botframework.dispatcher;

/**
 * Signals conflicting writes to request-scoped runtime data container.
 */
public final class RuntimeDataConflictException extends IllegalStateException {
    private final String keyName;
    private final Object existingValue;
    private final Object incomingValue;
    private final RuntimeDataScope scope;

    RuntimeDataConflictException(
            String keyName,
            Object existingValue,
            Object incomingValue,
            RuntimeDataScope scope
    ) {
        super("runtime data conflict on key '%s' in scope %s".formatted(keyName, scope));
        this.keyName = keyName;
        this.existingValue = existingValue;
        this.incomingValue = incomingValue;
        this.scope = scope;
    }

    String keyName() {
        return keyName;
    }

    Object existingValue() {
        return existingValue;
    }

    Object incomingValue() {
        return incomingValue;
    }

    RuntimeDataScope scope() {
        return scope;
    }
}
