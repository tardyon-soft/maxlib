package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.Objects;

/**
 * Logical screen button that can either invoke screen/widget callback action or map to a typed inline button.
 */
public record ScreenButton(
        Kind kind,
        String text,
        String action,
        Map<String, String> args,
        String payload
) {
    public ScreenButton {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(args, "args");
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        args = Map.copyOf(args);

        switch (kind) {
            case CALLBACK -> {
                Objects.requireNonNull(action, "action");
                if (action.isBlank()) {
                    throw new IllegalArgumentException("action must not be blank");
                }
            }
            case CLIPBOARD, LINK, OPEN_APP, MESSAGE -> {
                Objects.requireNonNull(payload, "payload");
                if (payload.isBlank()) {
                    throw new IllegalArgumentException("payload must not be blank");
                }
            }
            case REQUEST_CONTACT, REQUEST_GEO_LOCATION -> {
                // no extra payload required
            }
        }
    }

    public static ScreenButton of(String text, String action) {
        return callback(text, action);
    }

    public static ScreenButton callback(String text, String action) {
        return callback(text, action, Map.of());
    }

    public static ScreenButton callback(String text, String action, Map<String, String> args) {
        return new ScreenButton(Kind.CALLBACK, text, action, args, null);
    }

    public static ScreenButton clipboard(String text, String payload) {
        return new ScreenButton(Kind.CLIPBOARD, text, null, Map.of(), payload);
    }

    public static ScreenButton link(String text, String url) {
        return new ScreenButton(Kind.LINK, text, null, Map.of(), url);
    }

    public static ScreenButton requestContact(String text) {
        return new ScreenButton(Kind.REQUEST_CONTACT, text, null, Map.of(), null);
    }

    public static ScreenButton requestGeoLocation(String text) {
        return new ScreenButton(Kind.REQUEST_GEO_LOCATION, text, null, Map.of(), null);
    }

    public static ScreenButton openApp(String text, String appId) {
        return new ScreenButton(Kind.OPEN_APP, text, null, Map.of(), appId);
    }

    public static ScreenButton message(String text, String messageText) {
        return new ScreenButton(Kind.MESSAGE, text, null, Map.of(), messageText);
    }

    public enum Kind {
        CALLBACK,
        CLIPBOARD,
        LINK,
        REQUEST_CONTACT,
        REQUEST_GEO_LOCATION,
        OPEN_APP,
        MESSAGE
    }
}
