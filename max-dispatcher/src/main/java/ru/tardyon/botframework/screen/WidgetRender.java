package ru.tardyon.botframework.screen;

import java.util.List;

/**
 * Materialized widget output.
 */
public record WidgetRender(
        List<String> textLines,
        List<List<ScreenButton>> buttons
) {
    public WidgetRender {
        textLines = textLines == null ? List.of() : List.copyOf(textLines);
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
    }

    public static WidgetRender of(List<String> textLines, List<List<ScreenButton>> buttons) {
        return new WidgetRender(textLines, buttons);
    }
}
