package ru.tardyon.botframework.screen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.screen.annotation.OnAction;
import ru.tardyon.botframework.screen.annotation.OnText;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

/**
 * Registers annotation-driven screen declarations on top of existing {@link ScreenDefinition} API.
 */
public final class AnnotatedScreenRegistrar {
    private static final Logger log = LoggerFactory.getLogger(AnnotatedScreenRegistrar.class);

    public ScreenDefinition register(Object screenObject) {
        Objects.requireNonNull(screenObject, "screenObject");
        Class<?> type = screenObject.getClass();
        Screen annotation = type.getAnnotation(Screen.class);
        if (annotation == null) {
            throw new IllegalArgumentException("screen object must be annotated with @Screen");
        }
        String screenId = normalizeScreenId(annotation.value());
        Method render = null;
        Method onText = null;
        LinkedHashMap<String, Method> actions = new LinkedHashMap<>();

        for (Method method : type.getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.isAnnotationPresent(Render.class)) {
                if (render != null) {
                    throw new IllegalStateException("screen must declare exactly one @Render method: " + type.getName());
                }
                render = method;
            }
            OnText onTextAnnotation = method.getAnnotation(OnText.class);
            if (onTextAnnotation != null) {
                if (onText != null) {
                    throw new IllegalStateException("screen must declare at most one @OnText method: " + type.getName());
                }
                onText = method;
            }
            OnAction onActionAnnotation = method.getAnnotation(OnAction.class);
            if (onActionAnnotation != null) {
                String action = normalizeAction(onActionAnnotation.value());
                if (actions.containsKey(action)) {
                    throw new IllegalStateException("duplicate @OnAction mapping: " + action + " in " + type.getName());
                }
                actions.put(action, method);
            }
        }

        if (render == null) {
            throw new IllegalStateException("screen must declare @Render method: " + type.getName());
        }
        makeAccessible(screenObject, render);
        if (onText != null) {
            makeAccessible(screenObject, onText);
        }
        actions.values().forEach(method -> makeAccessible(screenObject, method));

        log.debug("Registered annotated screen: type={}, id={}, actions={}, hasOnText={}",
                type.getName(),
                screenId,
                actions.keySet(),
                onText != null);
        return new ReflectiveScreenDefinition(screenId, screenObject, render, onText, Map.copyOf(actions));
    }

    private static void makeAccessible(Object target, Method method) {
        if (!method.canAccess(target)) {
            method.setAccessible(true);
        }
    }

    private static String normalizeScreenId(String value) {
        String normalized = Objects.requireNonNull(value, "screen id").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("@Screen value must not be blank");
        }
        return normalized;
    }

    private static String normalizeAction(String value) {
        String normalized = Objects.requireNonNull(value, "action").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("@OnAction value must not be blank");
        }
        return normalized;
    }

    private static final class ReflectiveScreenDefinition implements ScreenDefinition {
        private final String id;
        private final Object target;
        private final Method render;
        private final Method onText;
        private final Map<String, Method> actions;

        private ReflectiveScreenDefinition(
                String id,
                Object target,
                Method render,
                Method onText,
                Map<String, Method> actions
        ) {
            this.id = id;
            this.target = target;
            this.render = render;
            this.onText = onText;
            this.actions = actions;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public CompletionStage<ScreenModel> render(ScreenContext context) {
            Object result = invoke(render, context, null, null, true);
            if (result instanceof CompletionStage<?> stage) {
                return stage.thenApply(value -> castRenderResult(render, value));
            }
            return CompletableFuture.completedFuture(castRenderResult(render, result));
        }

        @Override
        public CompletionStage<Void> onAction(ScreenContext context, String action, Map<String, String> args) {
            Method method = actions.get(action);
            if (method == null) {
                return CompletableFuture.completedFuture(null);
            }
            return castVoidResult(method, invoke(method, context, action, args, false));
        }

        @Override
        public CompletionStage<Void> onText(ScreenContext context, String text) {
            if (onText == null) {
                return CompletableFuture.completedFuture(null);
            }
            return castVoidResult(onText, invoke(onText, context, null, text, false));
        }

        private Object invoke(Method method, ScreenContext context, String action, Object payload, boolean renderInvocation) {
            Object[] args = resolveArguments(method, context, action, payload, renderInvocation);
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access method: " + method, e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw new IllegalStateException("Annotated screen method failed: " + method, cause);
            }
        }

        private static Object[] resolveArguments(
                Method method,
                ScreenContext context,
                String action,
                Object payload,
                boolean renderInvocation
        ) {
            Class<?>[] types = method.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];
                if (ScreenContext.class.isAssignableFrom(type)) {
                    args[i] = context;
                    continue;
                }
                if (Map.class.isAssignableFrom(type) && payload instanceof Map<?, ?> mapPayload) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (Map<String, String>) mapPayload;
                    args[i] = map;
                    continue;
                }
                if (String.class.equals(type)) {
                    if (renderInvocation) {
                        throw new IllegalStateException("Unsupported @Render method parameter: " + method);
                    }
                    args[i] = action != null ? action : payload;
                    continue;
                }
                throw new IllegalStateException("Unsupported annotated screen method parameter: " + method);
            }
            return args;
        }

        private static ScreenModel castRenderResult(Method method, Object value) {
            if (value instanceof ScreenModel model) {
                return model;
            }
            throw new IllegalStateException("@Render method must return ScreenModel or CompletionStage<ScreenModel>: " + method);
        }

        private static CompletionStage<Void> castVoidResult(Method method, Object value) {
            if (value == null) {
                return CompletableFuture.completedFuture(null);
            }
            if (value instanceof CompletionStage<?> stage) {
                return stage.thenApply(ignored -> null);
            }
            throw new IllegalStateException(
                    "Action/Text screen method must return void or CompletionStage: " + method
            );
        }
    }
}

