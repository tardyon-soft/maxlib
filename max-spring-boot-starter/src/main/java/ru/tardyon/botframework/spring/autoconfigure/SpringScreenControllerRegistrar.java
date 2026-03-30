package ru.tardyon.botframework.spring.autoconfigure;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenDefinition;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenText;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;
import ru.tardyon.botframework.spring.screen.annotation.ScreenView;

/**
 * Registers {@link ScreenController} facade methods into {@link ScreenDefinition} instances.
 */
final class SpringScreenControllerRegistrar {

    List<ScreenDefinition> register(Object controllerBean) {
        Objects.requireNonNull(controllerBean, "controllerBean");
        Class<?> type = controllerBean.getClass();
        ScreenController annotation = type.getAnnotation(ScreenController.class);
        if (annotation == null) {
            throw new IllegalArgumentException("controller bean must be annotated with @ScreenController");
        }

        Map<String, ScreenMethodGroup> byScreen = new LinkedHashMap<>();
        for (Method method : type.getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            ScreenView view = method.getAnnotation(ScreenView.class);
            if (view != null) {
                registerView(type, byScreen, normalize(view.screen(), "@ScreenView.screen"), method);
                continue;
            }
            OnScreenAction action = method.getAnnotation(OnScreenAction.class);
            if (action != null) {
                registerAction(
                        type,
                        byScreen,
                        normalize(action.screen(), "@OnScreenAction.screen"),
                        normalize(action.action(), "@OnScreenAction.action"),
                        method
                );
                continue;
            }
            OnScreenText onText = method.getAnnotation(OnScreenText.class);
            if (onText != null) {
                registerText(type, byScreen, normalize(onText.screen(), "@OnScreenText.screen"), method);
            }
        }

        if (byScreen.isEmpty()) {
            throw new IllegalStateException("ScreenController has no mapped methods: " + type.getName());
        }

        ArrayList<ScreenDefinition> definitions = new ArrayList<>();
        byScreen.forEach((screenId, group) -> {
            if (group.viewMethod == null) {
                throw new IllegalStateException("ScreenController screen has no @ScreenView method: " + screenId);
            }
            definitions.add(new ReflectiveScreenDefinition(screenId, controllerBean, group));
        });
        return List.copyOf(definitions);
    }

    private static void registerView(Class<?> type, Map<String, ScreenMethodGroup> byScreen, String screenId, Method method) {
        validateParameters(method, true);
        Class<?> returnType = method.getReturnType();
        if (!ScreenModel.class.isAssignableFrom(returnType) && !CompletionStage.class.isAssignableFrom(returnType)) {
            throw new IllegalStateException("@ScreenView must return ScreenModel or CompletionStage<ScreenModel>: " + method);
        }
        makeAccessible(method);
        ScreenMethodGroup group = byScreen.computeIfAbsent(screenId, ignored -> new ScreenMethodGroup());
        if (group.viewMethod != null) {
            throw new IllegalStateException("Duplicate @ScreenView for screen=" + screenId + " in " + type.getName());
        }
        group.viewMethod = method;
    }

    private static void registerAction(
            Class<?> type,
            Map<String, ScreenMethodGroup> byScreen,
            String screenId,
            String action,
            Method method
    ) {
        validateParameters(method, false);
        validateVoidLikeReturn(method, "@OnScreenAction");
        makeAccessible(method);
        ScreenMethodGroup group = byScreen.computeIfAbsent(screenId, ignored -> new ScreenMethodGroup());
        if (group.actions.containsKey(action)) {
            throw new IllegalStateException("Duplicate @OnScreenAction mapping: screen=" + screenId + ", action=" + action);
        }
        group.actions.put(action, method);
    }

    private static void registerText(Class<?> type, Map<String, ScreenMethodGroup> byScreen, String screenId, Method method) {
        validateParameters(method, false);
        validateVoidLikeReturn(method, "@OnScreenText");
        makeAccessible(method);
        ScreenMethodGroup group = byScreen.computeIfAbsent(screenId, ignored -> new ScreenMethodGroup());
        if (group.textMethod != null) {
            throw new IllegalStateException("Duplicate @OnScreenText mapping for screen=" + screenId + " in " + type.getName());
        }
        group.textMethod = method;
    }

    private static void validateParameters(Method method, boolean renderMethod) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            boolean supported = parameterType == ScreenContext.class
                    || parameterType == ru.tardyon.botframework.dispatcher.RuntimeContext.class
                    || parameterType == Message.class
                    || parameterType == Callback.class
                    || parameterType == String.class
                    || Map.class.isAssignableFrom(parameterType);
            if (!supported) {
                throw new IllegalStateException("Unsupported parameter for screen controller method: " + method);
            }
            if (renderMethod && (parameterType == String.class || Map.class.isAssignableFrom(parameterType))) {
                throw new IllegalStateException("Render method does not support String/Map payload parameters: " + method);
            }
        }
    }

    private static void validateVoidLikeReturn(Method method, String annotation) {
        Class<?> returnType = method.getReturnType();
        if (returnType == Void.TYPE || returnType == Void.class || CompletionStage.class.isAssignableFrom(returnType)) {
            return;
        }
        throw new IllegalStateException(annotation + " must return void/Void or CompletionStage: " + method);
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

    private static final class ScreenMethodGroup {
        private Method viewMethod;
        private Method textMethod;
        private final Map<String, Method> actions = new LinkedHashMap<>();
    }

    private static final class ReflectiveScreenDefinition implements ScreenDefinition {
        private final String id;
        private final Object bean;
        private final Method viewMethod;
        private final Method textMethod;
        private final Map<String, Method> actions;

        private ReflectiveScreenDefinition(String id, Object bean, ScreenMethodGroup group) {
            this.id = id;
            this.bean = bean;
            this.viewMethod = group.viewMethod;
            this.textMethod = group.textMethod;
            this.actions = Map.copyOf(group.actions);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public CompletionStage<ScreenModel> render(ScreenContext context) {
            Object value = invoke(viewMethod, context, null, Map.of());
            if (value instanceof CompletionStage<?> stage) {
                return stage.thenApply(result -> castScreenModel(viewMethod, result));
            }
            return CompletableFuture.completedFuture(castScreenModel(viewMethod, value));
        }

        @Override
        public CompletionStage<Void> onAction(ScreenContext context, String action, Map<String, String> args) {
            Method method = actions.get(action);
            if (method == null) {
                return CompletableFuture.completedFuture(null);
            }
            return castVoid(method, invoke(method, context, action, args));
        }

        @Override
        public CompletionStage<Void> onText(ScreenContext context, String text) {
            if (textMethod == null) {
                return CompletableFuture.completedFuture(null);
            }
            return castVoid(textMethod, invoke(textMethod, context, text, Map.of()));
        }

        private Object invoke(Method method, ScreenContext context, String stringPayload, Map<String, String> actionArgs) {
            Object[] args = resolveArgs(method, context, stringPayload, actionArgs);
            try {
                return method.invoke(bean, args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access screen controller method: " + method, e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw new IllegalStateException("Screen controller method failed: " + method, cause);
            }
        }

        private static Object[] resolveArgs(
                Method method,
                ScreenContext screenContext,
                String stringPayload,
                Map<String, String> actionArgs
        ) {
            Class<?>[] types = method.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];
                if (type == ScreenContext.class) {
                    args[i] = screenContext;
                } else if (type == ru.tardyon.botframework.dispatcher.RuntimeContext.class) {
                    args[i] = screenContext.runtime();
                } else if (type == Message.class) {
                    args[i] = extractMessage(screenContext);
                } else if (type == Callback.class) {
                    args[i] = screenContext.runtime().update().callback();
                } else if (type == String.class) {
                    args[i] = stringPayload;
                } else if (Map.class.isAssignableFrom(type)) {
                    args[i] = actionArgs;
                } else {
                    throw new IllegalStateException("Unsupported parameter for screen controller method: " + method);
                }
            }
            return args;
        }

        private static Message extractMessage(ScreenContext context) {
            if (context.runtime().update().message() != null) {
                return context.runtime().update().message();
            }
            if (context.runtime().update().callback() != null) {
                return context.runtime().update().callback().message();
            }
            return null;
        }

        private static ScreenModel castScreenModel(Method method, Object value) {
            if (value instanceof ScreenModel model) {
                return model;
            }
            throw new IllegalStateException("@ScreenView must return ScreenModel or CompletionStage<ScreenModel>: " + method);
        }

        private static CompletionStage<Void> castVoid(Method method, Object value) {
            if (value == null) {
                return CompletableFuture.completedFuture(null);
            }
            if (value instanceof CompletionStage<?> stage) {
                return stage.thenApply(ignored -> null);
            }
            throw new IllegalStateException("Method must return void/Void or CompletionStage: " + method);
        }
    }
}
