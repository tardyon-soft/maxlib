package ru.tardyon.botframework.dispatcher;

/**
 * Result status of one observer-level handler execution attempt.
 */
public enum HandlerExecutionStatus {
    HANDLED,
    IGNORED,
    FAILED
}

