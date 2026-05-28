package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ScreenButtonTest {

    @Test
    void createsCallbackButtonWithArgs() {
        ScreenButton button = ScreenButton.callback("Open", "profile", Map.of("id", "42"));

        assertEquals(ScreenButton.Kind.CALLBACK, button.kind());
        assertEquals("profile", button.action());
        assertEquals("42", button.args().get("id"));
    }

    @Test
    void createsTypedInlineButtons() {
        assertEquals(ScreenButton.Kind.CLIPBOARD, ScreenButton.clipboard("Copy", "payload").kind());
        assertEquals(ScreenButton.Kind.LINK, ScreenButton.link("Docs", "https://example.com").kind());
        assertEquals(ScreenButton.Kind.REQUEST_CONTACT, ScreenButton.requestContact("Contact").kind());
        assertEquals(ScreenButton.Kind.REQUEST_GEO_LOCATION, ScreenButton.requestGeoLocation("Location").kind());
        assertEquals(ScreenButton.Kind.OPEN_APP, ScreenButton.openApp("App", "app:orders").kind());
        assertEquals(ScreenButton.Kind.MESSAGE, ScreenButton.message("Send", "hello").kind());
    }

    @Test
    void validatesRequiredValues() {
        assertThrows(IllegalArgumentException.class, () -> ScreenButton.callback("Open", " "));
        assertThrows(IllegalArgumentException.class, () -> ScreenButton.clipboard("Copy", " "));
        assertThrows(IllegalArgumentException.class, () -> ScreenButton.link("Docs", " "));
        assertThrows(IllegalArgumentException.class, () -> ScreenButton.openApp("App", " "));
        assertThrows(IllegalArgumentException.class, () -> ScreenButton.message("Send", " "));
    }
}
