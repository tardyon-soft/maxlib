package ru.tardyon.botframework.dispatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

/**
 * MVP built-in filters for Sprint 4 runtime layer.
 */
public final class BuiltInFilters {
    public static final String COMMAND_KEY = "command";
    public static final String COMMAND_ARGS_KEY = "commandArgs";
    public static final String TEXT_SUFFIX_KEY = "textSuffix";
    public static final String USER_ID_KEY = "userId";
    public static final String CHAT_TYPE_KEY = "chatType";
    public static final String STATE_KEY = "state";

    private BuiltInFilters() {
    }

    /**
     * Matches slash command (for example {@code /start}) and exposes command + args enrichment.
     */
    public static Filter<Message> command(String command) {
        String normalized = normalizeCommand(command);
        return message -> CompletableFuture.completedFuture(matchCommand(message, normalized));
    }

    /**
     * Matches exact message text.
     */
    public static Filter<Message> textEquals(String text) {
        Objects.requireNonNull(text, "text");
        return message -> CompletableFuture.completedFuture(
                message != null && Objects.equals(message.text(), text)
                        ? FilterResult.matched()
                        : FilterResult.notMatched()
        );
    }

    /**
     * Matches message text prefix and exposes suffix as {@link #TEXT_SUFFIX_KEY}.
     */
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

    /**
     * Matches message chat type and exposes normalized type value as {@link #CHAT_TYPE_KEY}.
     */
    public static Filter<Message> chatType(ChatType expectedType) {
        Objects.requireNonNull(expectedType, "expectedType");
        return message -> {
            if (message == null || message.chat() == null || message.chat().type() != expectedType) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
            return CompletableFuture.completedFuture(FilterResult.matched(Map.of(CHAT_TYPE_KEY, expectedType.value())));
        };
    }

    /**
     * Matches message sender by user id and exposes user id as {@link #USER_ID_KEY}.
     */
    public static Filter<Message> fromUser(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return fromUser(userId, Message::from);
    }

    /**
     * Matches callback sender by user id and exposes user id as {@link #USER_ID_KEY}.
     */
    public static Filter<Callback> fromCallbackUser(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return fromUser(userId, Callback::from);
    }

    /**
     * Generic sender matcher for custom event types.
     */
    public static <TEvent> Filter<TEvent> fromUser(UserId userId, Function<TEvent, User> extractor) {
        return fromUserInternal(userId, extractor);
    }

    /**
     * Matches message with at least one attachment.
     */
    public static Filter<Message> hasAttachment() {
        return message -> CompletableFuture.completedFuture(
                message != null && message.attachments() != null && !message.attachments().isEmpty()
                        ? FilterResult.matched()
                        : FilterResult.notMatched()
        );
    }

    /**
     * Matches when current FSM state equals expected value.
     */
    public static <TEvent> Filter<TEvent> state(String state) {
        String expectedState = normalizeState(state);
        return stateIn(Set.of(expectedState));
    }

    /**
     * Matches when current FSM state is in expected set.
     */
    public static <TEvent> Filter<TEvent> stateIn(Set<String> states) {
        Objects.requireNonNull(states, "states");
        if (states.isEmpty()) {
            throw new IllegalArgumentException("states must not be empty");
        }
        Set<String> normalized = states.stream().map(BuiltInFilters::normalizeState).collect(java.util.stream.Collectors.toUnmodifiableSet());

        return new Filter<>() {
            @Override
            public java.util.concurrent.CompletionStage<FilterResult> test(TEvent event) {
                return CompletableFuture.completedFuture(FilterResult.failed(
                        new IllegalStateException("StateFilter requires runtime context with FSM support")
                ));
            }

            @Override
            public java.util.concurrent.CompletionStage<FilterResult> test(TEvent event, RuntimeContext context) {
                if (context == null) {
                    return CompletableFuture.completedFuture(FilterResult.failed(
                            new IllegalStateException("StateFilter requires runtime context with FSM support")
                    ));
                }
                return context.fsm().currentState()
                        .thenApply(current -> {
                            if (current.isEmpty()) {
                                return FilterResult.notMatched();
                            }
                            String value = current.orElseThrow();
                            if (!normalized.contains(value)) {
                                return FilterResult.notMatched();
                            }
                            return FilterResult.matched(Map.of(STATE_KEY, value));
                        })
                        .exceptionally(throwable -> FilterResult.failed(unwrap(throwable)));
            }
        };
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

    private static String normalizeState(String state) {
        Objects.requireNonNull(state, "state");
        String normalized = state.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("state must not be blank");
        }
        return normalized;
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof java.util.concurrent.CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
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
