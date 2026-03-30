package ru.tardyon.botframework.screen;

import java.util.List;
import java.util.Objects;
import ru.tardyon.botframework.model.request.NewMessageAttachment;

/**
 * Materialized widget view model that can be transformed into legacy {@link Widget}.
 */
public record WidgetView(
        List<String> textLines,
        List<List<ScreenButton>> buttons,
        List<NewMessageAttachment> attachments
) {
    public WidgetView {
        textLines = textLines == null ? List.of() : List.copyOf(textLines);
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public static WidgetView of(List<String> textLines, List<List<ScreenButton>> buttons) {
        return new WidgetView(textLines, buttons, List.of());
    }

    public static WidgetView of(
            List<String> textLines,
            List<List<ScreenButton>> buttons,
            List<NewMessageAttachment> attachments
    ) {
        return new WidgetView(textLines, buttons, attachments);
    }

    public Widget asWidget() {
        return context -> java.util.concurrent.CompletableFuture.completedFuture(
                WidgetRender.of(textLines, buttons, attachments)
        );
    }

    public static WidgetView fromWidgetRender(WidgetRender render) {
        Objects.requireNonNull(render, "render");
        return new WidgetView(render.textLines(), render.buttons(), render.attachments());
    }
}

