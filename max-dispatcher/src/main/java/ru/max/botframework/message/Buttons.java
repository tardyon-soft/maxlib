package ru.max.botframework.message;

/**
 * Factory methods for inline keyboard buttons.
 */
public final class Buttons {
    private Buttons() {
    }

    public static InlineKeyboardButton callback(String text, String callbackData) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.CALLBACK, text, callbackData);
    }

    public static InlineKeyboardButton link(String text, String url) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.LINK, text, url);
    }
}
