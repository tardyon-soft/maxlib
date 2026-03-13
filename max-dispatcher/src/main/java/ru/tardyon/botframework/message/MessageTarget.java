package ru.tardyon.botframework.message;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

/**
 * High-level message target abstraction for send/reply flows.
 *
 * <p>Target can point to either chat or user. For SDK send/edit operations framework resolves target
 * to {@link ChatId}.</p>
 */
public sealed interface MessageTarget permits MessageTarget.ChatTarget, MessageTarget.UserTarget {

    static MessageTarget chat(ChatId chatId) {
        return new ChatTarget(chatId);
    }

    static MessageTarget user(UserId userId) {
        return new UserTarget(userId);
    }

    Kind kind();

    Optional<ChatId> chatId();

    Optional<UserId> userId();

    /**
     * Resolves target to chat id suitable for low-level SDK requests.
     *
     * <p>Chat target returns its own {@link ChatId}.
     * User target uses resolver function to map user to chat.</p>
     */
    ChatId toChatId(UserChatResolver resolver);

    enum Kind {
        CHAT,
        USER
    }

    @FunctionalInterface
    interface UserChatResolver {
        ChatId resolve(UserId userId);
    }

    record ChatTarget(ChatId value) implements MessageTarget {
        public ChatTarget {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Kind kind() {
            return Kind.CHAT;
        }

        @Override
        public Optional<ChatId> chatId() {
            return Optional.of(value);
        }

        @Override
        public Optional<UserId> userId() {
            return Optional.empty();
        }

        @Override
        public ChatId toChatId(UserChatResolver resolver) {
            return value;
        }
    }

    record UserTarget(UserId value) implements MessageTarget {
        public UserTarget {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Kind kind() {
            return Kind.USER;
        }

        @Override
        public Optional<ChatId> chatId() {
            return Optional.empty();
        }

        @Override
        public Optional<UserId> userId() {
            return Optional.of(value);
        }

        @Override
        public ChatId toChatId(UserChatResolver resolver) {
            Objects.requireNonNull(resolver, "resolver");
            return Objects.requireNonNull(resolver.resolve(value), "resolved chatId");
        }
    }
}
