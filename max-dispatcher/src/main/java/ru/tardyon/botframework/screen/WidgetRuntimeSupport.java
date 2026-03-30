package ru.tardyon.botframework.screen;

import ru.tardyon.botframework.dispatcher.RuntimeDataKey;

final class WidgetRuntimeSupport {
    static final String WIDGET_ACTION_PREFIX = "__widget:";
    static final RuntimeDataKey<WidgetViewResolver> WIDGET_VIEW_RESOLVER_KEY =
            RuntimeDataKey.application("service:" + WidgetViewResolver.class.getName(), WidgetViewResolver.class);
    static final RuntimeDataKey<WidgetActionDispatcher> WIDGET_ACTION_DISPATCHER_KEY =
            RuntimeDataKey.application("service:" + WidgetActionDispatcher.class.getName(), WidgetActionDispatcher.class);

    private WidgetRuntimeSupport() {
    }

    static String encodeAction(String widgetId, String action) {
        return WIDGET_ACTION_PREFIX + widgetId + ":" + action;
    }

    static ParsedWidgetAction parseAction(String value) {
        if (value == null || !value.startsWith(WIDGET_ACTION_PREFIX)) {
            return null;
        }
        String raw = value.substring(WIDGET_ACTION_PREFIX.length());
        int separator = raw.indexOf(':');
        if (separator <= 0 || separator >= raw.length() - 1) {
            return null;
        }
        String widgetId = raw.substring(0, separator);
        String action = raw.substring(separator + 1);
        if (widgetId.isBlank() || action.isBlank()) {
            return null;
        }
        return new ParsedWidgetAction(widgetId, action);
    }

    record ParsedWidgetAction(String widgetId, String action) {
    }
}

