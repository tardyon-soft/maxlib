package ru.tardyon.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ButtonsTest {

    @Test
    void createsCallbackButton() {
        InlineKeyboardButton button = Buttons.callback("Pay", "pay:1");

        assertEquals(InlineKeyboardButton.Kind.CALLBACK, button.kind());
        assertEquals("pay:1", button.callbackData());
    }

    @Test
    void createsLinkButton() {
        InlineKeyboardButton button = Buttons.link("Docs", "https://example.com/docs");

        assertEquals(InlineKeyboardButton.Kind.LINK, button.kind());
        assertEquals("https://example.com/docs", button.url());
    }

    @Test
    void createsRequestContactButton() {
        InlineKeyboardButton button = Buttons.requestContact("Contact");

        assertEquals(InlineKeyboardButton.Kind.REQUEST_CONTACT, button.kind());
    }

    @Test
    void createsRequestGeoLocationButton() {
        InlineKeyboardButton button = Buttons.requestGeoLocation("Location");

        assertEquals(InlineKeyboardButton.Kind.REQUEST_GEO_LOCATION, button.kind());
    }

    @Test
    void createsOpenAppButton() {
        InlineKeyboardButton button = Buttons.openApp("Open", "app:orders");

        assertEquals(InlineKeyboardButton.Kind.OPEN_APP, button.kind());
        assertEquals("app:orders", button.openApp());
    }

    @Test
    void createsMessageButton() {
        InlineKeyboardButton button = Buttons.message("Send", "hello");

        assertEquals(InlineKeyboardButton.Kind.MESSAGE, button.kind());
        assertEquals("hello", button.message());
    }

    @Test
    void validatesRequiredPayloadForTypedButtons() {
        assertThrows(IllegalArgumentException.class, () -> Buttons.callback("Pay", " "));
        assertThrows(IllegalArgumentException.class, () -> Buttons.link("Site", " "));
        assertThrows(IllegalArgumentException.class, () -> Buttons.openApp("Open", " "));
        assertThrows(IllegalArgumentException.class, () -> Buttons.message("Send", " "));
    }

    @Test
    void validatesLinkUrlLength() {
        String tooLong = "h".repeat(InlineKeyboardConstraints.MAX_LINK_URL_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> Buttons.link("Site", tooLong));
    }
}
