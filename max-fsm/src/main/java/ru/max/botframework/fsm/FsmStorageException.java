package ru.max.botframework.fsm;

/**
 * Signals failure on FSM/scenes storage boundary.
 */
public final class FsmStorageException extends RuntimeException {

    public FsmStorageException(String operation, Throwable cause) {
        super("FSM storage operation failed: " + operation, cause);
    }
}
