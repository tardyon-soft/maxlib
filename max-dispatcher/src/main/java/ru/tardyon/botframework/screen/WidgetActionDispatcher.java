package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Dispatches widget actions in runtime.
 */
public interface WidgetActionDispatcher {
    CompletionStage<WidgetEffect> dispatch(WidgetContext context, String action, Map<String, String> args);
}

