package ru.tardyon.botframework.fsm;

import java.util.Objects;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

/**
 * Stable identity key for reading/writing FSM state.
 */
public record StateKey(StateScope scope, UserId userId, ChatId chatId) {

    public StateKey {
        Objects.requireNonNull(scope, "scope");
        validateScope(scope, userId, chatId);
    }

    public static StateKey user(UserId userId) {
        return new StateKey(StateScope.USER, Objects.requireNonNull(userId, "userId"), null);
    }

    public static StateKey chat(ChatId chatId) {
        return new StateKey(StateScope.CHAT, null, Objects.requireNonNull(chatId, "chatId"));
    }

    public static StateKey userInChat(UserId userId, ChatId chatId) {
        return new StateKey(
                StateScope.USER_IN_CHAT,
                Objects.requireNonNull(userId, "userId"),
                Objects.requireNonNull(chatId, "chatId")
        );
    }

    private static void validateScope(StateScope scope, UserId userId, ChatId chatId) {
        switch (scope) {
            case USER -> {
                if (userId == null || chatId != null) {
                    throw new IllegalArgumentException("USER scope requires userId and forbids chatId");
                }
            }
            case CHAT -> {
                if (chatId == null || userId != null) {
                    throw new IllegalArgumentException("CHAT scope requires chatId and forbids userId");
                }
            }
            case USER_IN_CHAT -> {
                if (userId == null || chatId == null) {
                    throw new IllegalArgumentException("USER_IN_CHAT scope requires both userId and chatId");
                }
            }
        }
    }
}
