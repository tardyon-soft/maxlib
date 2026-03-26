package ru.tardyon.botframework.screen;

import java.util.concurrent.CompletionStage;

/**
 * Screen widget.
 */
@FunctionalInterface
public interface Widget {
    CompletionStage<WidgetRender> render(ScreenContext context);
}
