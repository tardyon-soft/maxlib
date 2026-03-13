package ru.max.botframework.fsm;

/**
 * Raised when a state key cannot be derived from an incoming update.
 */
public final class StateKeyResolutionException extends RuntimeException {

    public StateKeyResolutionException(String message) {
        super(message);
    }
}
