package ru.tardyon.botframework.fsm;

import java.util.Objects;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UserId;

/**
 * Built-in state key strategies for core scope modes.
 */
public final class StateKeyStrategies {

    private static final StateKeyStrategy USER = new DefaultStateKeyStrategy(StateScope.USER);
    private static final StateKeyStrategy CHAT = new DefaultStateKeyStrategy(StateScope.CHAT);
    private static final StateKeyStrategy USER_IN_CHAT = new DefaultStateKeyStrategy(StateScope.USER_IN_CHAT);

    private StateKeyStrategies() {
    }

    public static StateKeyStrategy user() {
        return USER;
    }

    public static StateKeyStrategy chat() {
        return CHAT;
    }

    public static StateKeyStrategy userInChat() {
        return USER_IN_CHAT;
    }

    public static StateKeyStrategy forScope(StateScope scope) {
        Objects.requireNonNull(scope, "scope");
        return switch (scope) {
            case USER -> USER;
            case CHAT -> CHAT;
            case USER_IN_CHAT -> USER_IN_CHAT;
        };
    }

    private static final class DefaultStateKeyStrategy implements StateKeyStrategy {
        private final StateScope scope;

        private DefaultStateKeyStrategy(StateScope scope) {
            this.scope = scope;
        }

        @Override
        public StateScope scope() {
            return scope;
        }

        @Override
        public StateKey resolve(Update update) {
            Objects.requireNonNull(update, "update");

            UserId userId = extractUserId(update);
            ChatId chatId = extractChatId(update);

            return switch (scope) {
                case USER -> {
                    if (userId == null) {
                        throw new StateKeyResolutionException("Unable to resolve USER scope key: missing user id");
                    }
                    yield StateKey.user(userId);
                }
                case CHAT -> {
                    if (chatId == null) {
                        throw new StateKeyResolutionException("Unable to resolve CHAT scope key: missing chat id");
                    }
                    yield StateKey.chat(chatId);
                }
                case USER_IN_CHAT -> {
                    if (userId == null || chatId == null) {
                        throw new StateKeyResolutionException(
                                "Unable to resolve USER_IN_CHAT scope key: missing user id or chat id"
                        );
                    }
                    yield StateKey.userInChat(userId, chatId);
                }
            };
        }

        private static UserId extractUserId(Update update) {
            Message message = update.message();
            if (message != null && message.from() != null) {
                return message.from().id();
            }

            Callback callback = update.callback();
            if (callback != null && callback.from() != null) {
                return callback.from().id();
            }

            return null;
        }

        private static ChatId extractChatId(Update update) {
            Message message = update.message();
            if (message != null && message.chat() != null) {
                return message.chat().id();
            }

            Callback callback = update.callback();
            if (callback != null && callback.message() != null && callback.message().chat() != null) {
                return callback.message().chat().id();
            }

            return null;
        }
    }
}
