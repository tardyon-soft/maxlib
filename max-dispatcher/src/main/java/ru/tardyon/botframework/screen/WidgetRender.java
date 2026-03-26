package ru.tardyon.botframework.screen;

import java.util.List;
import ru.tardyon.botframework.model.request.NewMessageAttachment;

/**
 * Materialized widget output.
 */
public record WidgetRender(
        List<String> textLines,
        List<List<ScreenButton>> buttons,
        List<NewMessageAttachment> attachments
) {
    public WidgetRender {
        textLines = textLines == null ? List.of() : List.copyOf(textLines);
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public static WidgetRender of(List<String> textLines, List<List<ScreenButton>> buttons) {
        return new WidgetRender(textLines, buttons, List.of());
    }

    public static WidgetRender of(
            List<String> textLines,
            List<List<ScreenButton>> buttons,
            List<NewMessageAttachment> attachments
    ) {
        return new WidgetRender(textLines, buttons, attachments);
    }
}
