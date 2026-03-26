package ru.tardyon.botframework.screen;

import java.util.concurrent.CompletionStage;

/**
 * Renders screen model into chat message.
 */
public interface ScreenRenderer {
    CompletionStage<RenderResult> render(ScreenContext context, ScreenModel model);
}
