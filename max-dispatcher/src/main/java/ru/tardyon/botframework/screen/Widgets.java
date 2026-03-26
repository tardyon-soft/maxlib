package ru.tardyon.botframework.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.message.MediaAttachment;
import ru.tardyon.botframework.model.request.NewMessageAttachment;

/**
 * Widget factory helpers.
 */
public final class Widgets {
    private Widgets() {
    }

    public static Widget text(String value) {
        Objects.requireNonNull(value, "value");
        return context -> CompletableFuture.completedFuture(WidgetRender.of(List.of(value), List.of()));
    }

    public static Widget spacer() {
        return context -> CompletableFuture.completedFuture(WidgetRender.of(List.of(""), List.of()));
    }

    public static Widget buttonRow(ScreenButton... buttons) {
        Objects.requireNonNull(buttons, "buttons");
        ArrayList<ScreenButton> row = new ArrayList<>(buttons.length);
        for (ScreenButton button : buttons) {
            row.add(Objects.requireNonNull(button, "button"));
        }
        return context -> CompletableFuture.completedFuture(WidgetRender.of(List.of(), List.of(List.copyOf(row))));
    }

    public static Widget media(MediaAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        return attachment(attachment.toNewMessageAttachment());
    }

    public static Widget attachment(NewMessageAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        return context -> CompletableFuture.completedFuture(WidgetRender.of(List.of(), List.of(), List.of(attachment)));
    }

    public static Widget attachments(List<NewMessageAttachment> attachments) {
        Objects.requireNonNull(attachments, "attachments");
        return context -> CompletableFuture.completedFuture(WidgetRender.of(List.of(), List.of(), List.copyOf(attachments)));
    }

    public static Widget section(String title, Widget... children) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(children, "children");
        List<Widget> childList = List.of(children);

        return context -> {
            CompletionStage<List<WidgetRender>> renderedChildren = renderAll(context, childList);
            return renderedChildren.thenApply(renders -> {
                ArrayList<String> lines = new ArrayList<>();
                ArrayList<List<ScreenButton>> buttons = new ArrayList<>();
                ArrayList<NewMessageAttachment> attachments = new ArrayList<>();
                lines.add(title);
                for (WidgetRender render : renders) {
                    lines.addAll(render.textLines());
                    buttons.addAll(render.buttons());
                    attachments.addAll(render.attachments());
                }
                return WidgetRender.of(lines, buttons, attachments);
            });
        };
    }

    private static CompletionStage<List<WidgetRender>> renderAll(ScreenContext context, List<Widget> widgets) {
        CompletionStage<List<WidgetRender>> stage = CompletableFuture.completedFuture(new ArrayList<>());
        for (Widget widget : widgets) {
            stage = stage.thenCompose(renders ->
                    widget.render(context).thenApply(render -> {
                        renders.add(render);
                        return renders;
                    }));
        }
        return stage.thenApply(List::copyOf);
    }
}
