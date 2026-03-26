package ru.tardyon.botframework.screen;

/**
 * Rendering outcome.
 */
public record RenderResult(
        String messageId,
        boolean edited
) {
}
