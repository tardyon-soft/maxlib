package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Resolves widget id to concrete {@link WidgetView} in runtime.
 */
public interface WidgetViewResolver {
    CompletionStage<WidgetView> resolve(WidgetContext context, Map<String, Object> params);
}

