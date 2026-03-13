package ru.max.botframework.message;

/**
 * Factory methods for inline keyboard buttons.
 */
public final class Buttons {
    private Buttons() {
    }

    public static InlineKeyboardButton callback(String text, String callbackData) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.CALLBACK, text, callbackData, null, null, null);
    }

    public static InlineKeyboardButton link(String text, String url) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.LINK, text, null, url, null, null);
    }

    public static InlineKeyboardButton requestContact(String text) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.REQUEST_CONTACT, text, null, null, null, null);
    }

    public static InlineKeyboardButton requestGeoLocation(String text) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.REQUEST_GEO_LOCATION, text, null, null, null, null);
    }

    public static InlineKeyboardButton openApp(String text, String appId) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.OPEN_APP, text, null, null, appId, null);
    }

    public static InlineKeyboardButton message(String text, String messageText) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.MESSAGE, text, null, null, null, messageText);
    }
}
