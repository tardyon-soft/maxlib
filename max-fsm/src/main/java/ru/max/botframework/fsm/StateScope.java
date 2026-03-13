package ru.max.botframework.fsm;

/**
 * Defines how FSM state is isolated across incoming updates.
 */
public enum StateScope {
    USER,
    CHAT,
    USER_IN_CHAT
}
