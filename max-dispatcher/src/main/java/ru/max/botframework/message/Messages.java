package ru.max.botframework.message;

/**
 * Factory entrypoints for high-level outgoing message builders.
 */
public final class Messages {
    private Messages() {
    }

    public static MessageBuilder message() {
        return MessageBuilder.empty();
    }

    public static MessageBuilder text(String text) {
        return MessageBuilder.textOnly(text);
    }
}
