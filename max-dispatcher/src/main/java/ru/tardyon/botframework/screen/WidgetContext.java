package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.Message;

/**
 * Runtime widget context available to widget render/action handlers.
 */
public record WidgetContext(
        ScreenContext screen,
        String widgetId,
        Map<String, Object> viewParams,
        Message message,
        Callback callback
) {
    public WidgetContext {
        screen = Objects.requireNonNull(screen, "screen");
        widgetId = Objects.requireNonNull(widgetId, "widgetId");
        viewParams = viewParams == null ? Map.of() : Map.copyOf(viewParams);
        if (widgetId.isBlank()) {
            throw new IllegalArgumentException("widgetId must not be blank");
        }
    }

    public RuntimeContext runtime() {
        return screen.runtime();
    }
}

