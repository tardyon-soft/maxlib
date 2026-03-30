package ru.tardyon.botframework.spring.widget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.WidgetActionDispatcher;
import ru.tardyon.botframework.screen.WidgetActions;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.screen.WidgetViewResolver;
import ru.tardyon.botframework.spring.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.spring.widget.annotation.Widget;
import ru.tardyon.botframework.spring.widget.annotation.WidgetController;

/**
 * Annotation-driven widget registry bridging widget ids to runtime render/action handlers.
 */
public final class AnnotatedWidgetRegistry implements WidgetViewResolver, WidgetActionDispatcher {
    private final Map<String, RegisteredWidget> widgets = new ConcurrentHashMap<>();

    public void register(Object controller) {
        Objects.requireNonNull(controller, "controller");
        Class<?> type = controller.getClass();
        WidgetController annotation = type.getAnnotation(WidgetController.class);
        if (annotation == null) {
            throw new IllegalArgumentException("widget controller must be annotated with @WidgetController");
        }

        Map<String, Method> renderByWidget = new LinkedHashMap<>();
        Map<String, Map<String, Method>> actionByWidget = new LinkedHashMap<>();

        for (Method method : type.getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Widget renderAnnotation = method.getAnnotation(Widget.class);
            if (renderAnnotation != null) {
                String widgetId = normalize(renderAnnotation.id(), "@Widget.id");
                validateRenderMethod(method);
                makeAccessible(method);
                if (renderByWidget.putIfAbsent(widgetId, method) != null) {
                    throw new IllegalStateException("Duplicate @Widget id mapping: " + widgetId + " in " + type.getName());
                }
            }

            OnWidgetAction actionAnnotation = method.getAnnotation(OnWidgetAction.class);
            if (actionAnnotation != null) {
                String widgetId = normalize(actionAnnotation.widget(), "@OnWidgetAction.widget");
                String action = normalize(actionAnnotation.action(), "@OnWidgetAction.action");
                validateActionMethod(method);
                makeAccessible(method);
                Map<String, Method> actionMap = actionByWidget.computeIfAbsent(widgetId, ignored -> new LinkedHashMap<>());
                if (actionMap.putIfAbsent(action, method) != null) {
                    throw new IllegalStateException(
                            "Duplicate @OnWidgetAction mapping: widget=" + widgetId + ", action=" + action + " in " + type.getName()
                    );
                }
            }
        }

        if (renderByWidget.isEmpty()) {
            throw new IllegalStateException("WidgetController has no @Widget render methods: " + type.getName());
        }

        renderByWidget.forEach((widgetId, renderMethod) -> {
            RegisteredWidget registered = new RegisteredWidget(
                    controller,
                    renderMethod,
                    Map.copyOf(actionByWidget.getOrDefault(widgetId, Map.of()))
            );
            RegisteredWidget existing = widgets.putIfAbsent(widgetId, registered);
            if (existing != null) {
                throw new IllegalStateException("Widget id already registered: " + widgetId);
            }
        });
    }

    @Override
    public CompletionStage<WidgetView> resolve(WidgetContext context, Map<String, Object> params) {
        RegisteredWidget widget = widgets.get(context.widgetId());
        if (widget == null) {
            return CompletableFuture.completedFuture(WidgetView.of(
                    java.util.List.of("Widget not found: " + context.widgetId()),
                    java.util.List.of()
            ));
        }
        Object value = invoke(widget.controller, widget.renderMethod, context, null, params);
        CompletionStage<WidgetView> viewStage = castWidgetView(widget.renderMethod, value);
        return viewStage.thenApply(view -> encodeButtonActions(context.widgetId(), view));
    }

    @Override
    public CompletionStage<WidgetEffect> dispatch(WidgetContext context, String action, Map<String, String> args) {
        RegisteredWidget widget = widgets.get(context.widgetId());
        if (widget == null) {
            return CompletableFuture.completedFuture(WidgetEffect.NONE);
        }
        Method method = widget.actions.get(action);
        if (method == null) {
            return CompletableFuture.completedFuture(WidgetEffect.NONE);
        }
        Object value = invoke(widget.controller, method, context, action, args);
        return castEffect(method, value);
    }

    private static CompletionStage<WidgetView> castWidgetView(Method method, Object value) {
        if (value instanceof CompletionStage<?> stage) {
            return stage.thenApply(result -> {
                if (result instanceof WidgetView widgetView) {
                    return widgetView;
                }
                throw new IllegalStateException("@Widget must return WidgetView or CompletionStage<WidgetView>: " + method);
            });
        }
        if (value instanceof WidgetView widgetView) {
            return CompletableFuture.completedFuture(widgetView);
        }
        throw new IllegalStateException("@Widget must return WidgetView or CompletionStage<WidgetView>: " + method);
    }

    private static CompletionStage<WidgetEffect> castEffect(Method method, Object value) {
        if (value == null) {
            return CompletableFuture.completedFuture(WidgetEffect.RERENDER);
        }
        if (value instanceof WidgetEffect effect) {
            return CompletableFuture.completedFuture(effect);
        }
        if (value instanceof CompletionStage<?> stage) {
            return stage.thenApply(result -> {
                if (result == null) {
                    return WidgetEffect.RERENDER;
                }
                if (result instanceof WidgetEffect effect) {
                    return effect;
                }
                throw new IllegalStateException(
                        "@OnWidgetAction must return void/WidgetEffect or CompletionStage<Void/WidgetEffect>: " + method
                );
            });
        }
        throw new IllegalStateException("@OnWidgetAction return type is not supported: " + method);
    }

    private static WidgetView encodeButtonActions(String widgetId, WidgetView view) {
        java.util.ArrayList<java.util.List<ScreenButton>> rows = new java.util.ArrayList<>();
        for (java.util.List<ScreenButton> row : view.buttons()) {
            java.util.ArrayList<ScreenButton> mappedRow = new java.util.ArrayList<>();
            for (ScreenButton button : row) {
                mappedRow.add(new ScreenButton(
                        button.text(),
                        WidgetActions.callbackAction(widgetId, button.action()),
                        button.args()
                ));
            }
            rows.add(java.util.List.copyOf(mappedRow));
        }
        return WidgetView.of(view.textLines(), java.util.List.copyOf(rows), view.attachments());
    }

    private static Object invoke(
            Object controller,
            Method method,
            WidgetContext context,
            String action,
            Object payload
    ) {
        Object[] args = resolveArgs(method, context, action, payload);
        try {
            return method.invoke(controller, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access widget method: " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalStateException("Widget method failed: " + method, cause);
        }
    }

    private static Object[] resolveArgs(Method method, WidgetContext context, String action, Object payload) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type == WidgetContext.class) {
                args[i] = context;
            } else if (type == ru.tardyon.botframework.screen.ScreenContext.class) {
                args[i] = context.screen();
            } else if (type == ru.tardyon.botframework.dispatcher.RuntimeContext.class) {
                args[i] = context.runtime();
            } else if (type == Message.class) {
                args[i] = context.message();
            } else if (type == Callback.class) {
                args[i] = context.callback();
            } else if (type == String.class) {
                args[i] = action;
            } else if (Map.class.isAssignableFrom(type)) {
                args[i] = payload;
            } else {
                throw new IllegalStateException("Unsupported widget controller parameter: " + method);
            }
        }
        return args;
    }

    private static void validateRenderMethod(Method method) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (parameterType == String.class || Map.class.isAssignableFrom(parameterType)) {
                throw new IllegalStateException("@Widget render methods do not support String/Map payload params: " + method);
            }
            ensureSupportedParam(method, parameterType);
        }
        Class<?> returnType = method.getReturnType();
        if (!WidgetView.class.isAssignableFrom(returnType) && !CompletionStage.class.isAssignableFrom(returnType)) {
            throw new IllegalStateException("@Widget must return WidgetView or CompletionStage<WidgetView>: " + method);
        }
    }

    private static void validateActionMethod(Method method) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            ensureSupportedParam(method, parameterType);
        }
        Class<?> returnType = method.getReturnType();
        boolean valid = returnType == Void.TYPE
                || returnType == Void.class
                || returnType == WidgetEffect.class
                || CompletionStage.class.isAssignableFrom(returnType);
        if (!valid) {
            throw new IllegalStateException(
                    "@OnWidgetAction must return void/WidgetEffect or CompletionStage<Void/WidgetEffect>: " + method
            );
        }
    }

    private static void ensureSupportedParam(Method method, Class<?> parameterType) {
        boolean supported = parameterType == WidgetContext.class
                || parameterType == ru.tardyon.botframework.screen.ScreenContext.class
                || parameterType == ru.tardyon.botframework.dispatcher.RuntimeContext.class
                || parameterType == Message.class
                || parameterType == Callback.class
                || parameterType == String.class
                || Map.class.isAssignableFrom(parameterType);
        if (!supported) {
            throw new IllegalStateException("Unsupported widget controller parameter: " + method);
        }
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static void makeAccessible(Method method) {
        if (!method.trySetAccessible()) {
            method.setAccessible(true);
        }
    }

    private record RegisteredWidget(
            Object controller,
            Method renderMethod,
            Map<String, Method> actions
    ) {
    }
}

