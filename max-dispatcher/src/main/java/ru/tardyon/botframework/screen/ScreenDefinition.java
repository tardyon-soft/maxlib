package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Screen contract.
 */
public interface ScreenDefinition {
    String id();

    CompletionStage<ScreenModel> render(ScreenContext context);

    default CompletionStage<Void> onText(ScreenContext context, String text) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletionStage<Void> onAction(ScreenContext context, String action, Map<String, String> args) {
        return CompletableFuture.completedFuture(null);
    }
}
