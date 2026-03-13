package ru.max.botframework.fsm;

/**
 * Defines how FSM state is isolated across incoming updates.
 */
public enum StateScope {
    /** One state per user across all chats. */
    USER,
    /** One state per chat, independent from user identity. */
    CHAT,
    /** One state per (user, chat) pair. */
    USER_IN_CHAT
}
