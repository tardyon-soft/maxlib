package ru.max.botframework.fsm;

import java.util.Optional;

/**
 * Storage contract for per-user or per-chat finite state.
 */
public interface FsmStorage {
    Optional<String> getState(String scopeKey);

    void setState(String scopeKey, String state);

    void clearState(String scopeKey);
}
