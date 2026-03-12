package ru.max.botframework.dispatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;

/**
 * MVP built-in filters for Sprint 4 runtime layer.
 */
public final class BuiltInFilters {
    public static final String COMMAND_KEY = "command";
    public static final String COMMAND_ARGS_KEY = "commandArgs";
    public static final String TEXT_SUFFIX_KEY = "textSuffix";
    public static final String USER_ID_KEY = "userId";
    public static final String CHAT_TYPE_KEY = "chatType";

    private BuiltInFilters() {
    }

    public static Filter<Message> command(String command) {
        String normalized = normalizeCommand(command);
        return message -> CompletableFuture.completedFuture(matchCommand(message, normalized));
    }

    public static Filter<Message> textEquals(String text) {
        Objects.requireNonNull(text, "text");
        return message -> CompletableFuture.completedFuture(
                message != null && Objects.equals(message.text(), text)
                        ? FilterResult.matched()
                        : FilterResult.notMatched()
        );
    }

    public static Filter<Message> textStartsWith(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be empty");
        }
        return message -> {
            if (message == null || message.text() == null || !message.text().startsWith(prefix)) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
            String suffix = message.text().substring(prefix.length());
            return CompletableFuture.completedFuture(FilterResult.matched(Map.of(TEXT_SUFFIX_KEY, suffix)));
        };
    }

    public static Filter<Message> chatType(ChatType expectedType) {
        Objects.requireNonNull(expectedType, "expectedType");
        return message -> {
            if (message == null || message.chat() == null || message.chat().type() != expectedType) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
            return CompletableFuture.completedFuture(FilterResult.matched(Map.of(CHAT_TYPE_KEY, expectedType.value())));
        };
    }

    public static Filter<Message> fromUser(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return fromUser(userId, Message::from);
    }

    public static Filter<Callback> fromCallbackUser(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return fromUser(userId, Callback::from);
    }

    public static <TEvent> Filter<TEvent> fromUser(UserId userId, Function<TEvent, User> extractor) {
        return fromUserInternal(userId, extractor);
    }

    public static Filter<Message> hasAttachment() {
        return message -> CompletableFuture.completedFuture(
                message != null && message.attachments() != null && !message.attachments().isEmpty()
                        ? FilterResult.matched()
                        : FilterResult.notMatched()
        );
    }

    /**
     * Placeholder filter for FSM state matching until FSM runtime is introduced.
     */
    public static <TEvent> Filter<TEvent> state(String state) {
        Objects.requireNonNull(state, "state");
        return event -> CompletableFuture.completedFuture(
                FilterResult.failed(new UnsupportedOperationException(
                        "StateFilter requires FSM runtime, not available in Sprint 4.1"
                ))
        );
    }

    private static <TEvent> Filter<TEvent> fromUserInternal(UserId userId, Function<TEvent, User> extractor) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(extractor, "extractor");
        return event -> {
            if (event == null) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
            User user = extractor.apply(event);
            if (user == null || user.id() == null || !userId.equals(user.id())) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
            return CompletableFuture.completedFuture(FilterResult.matched(Map.of(USER_ID_KEY, user.id().value())));
        };
    }

    private static String normalizeCommand(String command) {
        Objects.requireNonNull(command, "command");
        String normalized = command.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        return normalized;
    }

    private static FilterResult matchCommand(Message message, String expectedCommand) {
        if (message == null || message.text() == null || message.text().isBlank()) {
            return FilterResult.notMatched();
        }
        String text = message.text().trim();
        if (!text.startsWith("/")) {
            return FilterResult.notMatched();
        }
        String payload = text.substring(1);
        String commandToken;
        String args;
        int firstSpace = payload.indexOf(' ');
        if (firstSpace < 0) {
            commandToken = payload;
            args = "";
        } else {
            commandToken = payload.substring(0, firstSpace);
            args = payload.substring(firstSpace + 1).trim();
        }
        if (!expectedCommand.equals(commandToken)) {
            return FilterResult.notMatched();
        }
        LinkedHashMap<String, Object> enrichment = new LinkedHashMap<>();
        enrichment.put(COMMAND_KEY, expectedCommand);
        enrichment.put(COMMAND_ARGS_KEY, args);
        return FilterResult.matched(enrichment);
    }
}
