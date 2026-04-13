package ru.tardyon.botframework.message;

/**
 * Factory methods for inline keyboard buttons.
 */
public final class Buttons {
    private Buttons() {
    }

    /**
     * Button that triggers callback update with opaque callback data.
     */
    public static InlineKeyboardButton callback(String text, String callbackData) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.CALLBACK, text, callbackData, null, null, null, null);
    }

    /**
     * Button that copies provided payload into clipboard.
     */
    public static InlineKeyboardButton clipboard(String text, String payload) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.CLIPBOARD, text, null, payload, null, null, null);
    }

    /**
     * Button that opens external URL.
     */
    public static InlineKeyboardButton link(String text, String url) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.LINK, text, null, null, url, null, null);
    }

    /**
     * Button that asks user to share contact.
     */
    public static InlineKeyboardButton requestContact(String text) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.REQUEST_CONTACT, text, null, null, null, null, null);
    }

    /**
     * Button that asks user to share geo location.
     */
    public static InlineKeyboardButton requestGeoLocation(String text) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.REQUEST_GEO_LOCATION, text, null, null, null, null, null);
    }

    /**
     * Button that opens MAX mini app.
     */
    public static InlineKeyboardButton openApp(String text, String appId) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.OPEN_APP, text, null, null, null, appId, null);
    }

    /**
     * Button that sends predefined message payload.
     */
    public static InlineKeyboardButton message(String text, String messageText) {
        return new InlineKeyboardButton(InlineKeyboardButton.Kind.MESSAGE, text, null, null, null, null, messageText);
    }
}
