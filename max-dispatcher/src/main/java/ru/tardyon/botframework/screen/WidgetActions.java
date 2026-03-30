package ru.tardyon.botframework.screen;

import java.util.Objects;

/**
 * Widget callback action helpers.
 */
public final class WidgetActions {
    private WidgetActions() {
    }

    public static String callbackAction(String widgetId, String action) {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(action, "action");
        String normalizedWidgetId = widgetId.trim();
        String normalizedAction = action.trim();
        if (normalizedWidgetId.isBlank()) {
            throw new IllegalArgumentException("widgetId must not be blank");
        }
        if (normalizedAction.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        return WidgetRuntimeSupport.encodeAction(normalizedWidgetId, normalizedAction);
    }
}

