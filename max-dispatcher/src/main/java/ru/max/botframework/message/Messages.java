package ru.max.botframework.message;

/**
 * Factory entrypoints for high-level outgoing message builders.
 *
 * <p>Factory does not perform network calls and only configures immutable {@link MessageBuilder} instances.</p>
 */
public final class Messages {
    private Messages() {
    }

    /**
     * Creates empty message builder for advanced composition (attachments-only, etc.).
     */
    public static MessageBuilder message() {
        return MessageBuilder.empty();
    }

    /**
     * Creates plain-text message builder.
     */
    public static MessageBuilder text(String text) {
        return MessageBuilder.textOnly(text);
    }

    /**
     * Creates message builder with explicit {@code plain} text format.
     */
    public static MessageBuilder plain(String text) {
        return text(text).plain();
    }

    /**
     * Creates message builder with explicit {@code markdown} text format.
     */
    public static MessageBuilder markdown(String text) {
        return text(text).markdown();
    }

    /**
     * Creates message builder with explicit {@code html} text format.
     */
    public static MessageBuilder html(String text) {
        return text(text).html();
    }
}
