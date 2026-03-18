package ru.tardyon.botframework.dispatcher;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Message;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.dispatcher.annotation.State;
import ru.tardyon.botframework.dispatcher.annotation.Text;
import ru.tardyon.botframework.dispatcher.annotation.UseFilters;
import ru.tardyon.botframework.dispatcher.annotation.UseMiddleware;

/**
 * Registers annotation-driven route declarations on top of existing {@link Router} API.
 */
public final class AnnotatedRouteRegistrar {
    private static final Logger log = LoggerFactory.getLogger(AnnotatedRouteRegistrar.class);
    private final ComponentResolver componentResolver;

    public AnnotatedRouteRegistrar() {
        this(ComponentResolver.reflectiveDefaults());
    }

    public AnnotatedRouteRegistrar(ComponentResolver componentResolver) {
        this.componentResolver = Objects.requireNonNull(componentResolver, "componentResolver");
    }

    public Router register(Object routeObject) {
        Objects.requireNonNull(routeObject, "routeObject");
        Route route = routeObject.getClass().getAnnotation(Route.class);
        if (route == null) {
            throw new IllegalArgumentException("route object must be annotated with @Route");
        }
        String routeName = normalizeRouteName(route.value());
        Router router = new Router(routeName);
        log.debug("Registering annotated route: type={}, name={}, autoRegister={}",
                routeObject.getClass().getName(),
                routeName,
                route.autoRegister());

        for (InnerMiddleware middleware : resolveMiddlewares(routeObject.getClass().getAnnotation(UseMiddleware.class))) {
            router.innerMiddleware(middleware);
        }

        Filter<?> classFilter = composeClassFilter(routeObject.getClass().getAnnotation(UseFilters.class));
        List<MethodRegistration> registrations = new ArrayList<>();
        for (Method method : routeObject.getClass().getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Registration registration = buildRegistration(routeObject, method, classFilter);
            if (registration == null) {
                continue;
            }
            registrations.add(new MethodRegistration(method, registration));
        }

        registrations.sort(Comparator
                .comparingInt((MethodRegistration value) -> value.registration().priority())
                .reversed()
                .thenComparing(value -> value.method().getName()));

        for (MethodRegistration methodRegistration : registrations) {
            Method method = methodRegistration.method();
            Registration registration = methodRegistration.registration();
            if (registration.eventType == EventType.MESSAGE) {
                registerMessage(router, registration);
                log.debug("Mapped annotated method to message handler: route={}, method={}", routeName, method.getName());
            } else {
                registerCallback(router, registration);
                log.debug("Mapped annotated method to callback handler: route={}, method={}", routeName, method.getName());
            }
        }

        log.debug("Annotated route registration completed: name={}, handlers(message={}, callback={})",
                routeName,
                router.messages().handlers().size(),
                router.callbacks().handlers().size());
        return router;
    }

    @SuppressWarnings("unchecked")
    private static void registerMessage(Router router, Registration registration) {
        ContextualEventHandler<ru.tardyon.botframework.model.Message> handler =
                (ContextualEventHandler<ru.tardyon.botframework.model.Message>) registration.handler;
        if (registration.filter == null) {
            router.message(handler);
            return;
        }
        router.message((Filter<ru.tardyon.botframework.model.Message>) registration.filter, handler);
    }

    @SuppressWarnings("unchecked")
    private static void registerCallback(Router router, Registration registration) {
        ContextualEventHandler<ru.tardyon.botframework.model.Callback> handler =
                (ContextualEventHandler<ru.tardyon.botframework.model.Callback>) registration.handler;
        if (registration.filter == null) {
            router.callback(handler);
            return;
        }
        router.callback((Filter<ru.tardyon.botframework.model.Callback>) registration.filter, handler);
    }

    private Registration buildRegistration(Object target, Method method, Filter<?> classFilter) {
        EventType eventType = resolveEventType(method);
        if (eventType == null) {
            return null;
        }
        if (!method.canAccess(target)) {
            method.setAccessible(true);
        }

        Filter<?> trigger = resolveTriggerFilter(method, eventType);
        Filter<?> methodFilters = composeMethodFilter(method.getAnnotation(UseFilters.class));
        Filter<?> merged = mergeFilters(classFilter, trigger);
        merged = mergeFilters(merged, methodFilters);

        ContextualEventHandler<?> handler = ReflectiveEventHandler.of(target, method);
        List<InnerMiddleware> methodMiddlewares = resolveMiddlewares(method.getAnnotation(UseMiddleware.class));
        if (!methodMiddlewares.isEmpty()) {
            handler = wrapWithMethodMiddlewares(handler, methodMiddlewares);
        }
        return new Registration(eventType, merged, handler, resolvePriority(method, eventType));
    }

    private EventType resolveEventType(Method method) {
        boolean messageHandler = hasAnyMessageMapping(method);
        boolean callbackHandler = hasAnyCallbackMapping(method);
        if (!messageHandler && !callbackHandler) {
            return null;
        }
        if (messageHandler && callbackHandler) {
            throw new IllegalStateException("method cannot mix message and callback annotations: " + method);
        }
        return messageHandler ? EventType.MESSAGE : EventType.CALLBACK;
    }

    private static boolean hasAnyMessageMapping(Method method) {
        return method.isAnnotationPresent(Message.class)
                || method.isAnnotationPresent(Text.class)
                || method.isAnnotationPresent(Command.class)
                || method.isAnnotationPresent(State.class);
    }

    private static boolean hasAnyCallbackMapping(Method method) {
        return method.isAnnotationPresent(ru.tardyon.botframework.dispatcher.annotation.Callback.class)
                || method.isAnnotationPresent(ru.tardyon.botframework.dispatcher.annotation.CallbackPrefix.class);
    }

    private static Filter<?> resolveTriggerFilter(Method method, EventType eventType) {
        Filter<?> filter = null;
        if (eventType == EventType.MESSAGE) {
            Message message = method.getAnnotation(Message.class);
            if (message != null && !message.text().isBlank()) {
                filter = mergeFilters(
                        filter,
                        message.startsWith()
                                ? BuiltInFilters.textStartsWith(message.text())
                                : BuiltInFilters.textEquals(message.text())
                );
            }

            Text text = method.getAnnotation(Text.class);
            if (text != null) {
                filter = mergeFilters(filter, BuiltInFilters.textEquals(text.value()));
            }

            Command command = method.getAnnotation(Command.class);
            if (command != null) {
                filter = mergeFilters(filter, BuiltInFilters.command(command.value()));
            }

            State state = method.getAnnotation(State.class);
            if (state != null) {
                filter = mergeFilters(filter, BuiltInFilters.state(state.value()));
            }
            return filter;
        }

        ru.tardyon.botframework.dispatcher.annotation.Callback callback =
                method.getAnnotation(ru.tardyon.botframework.dispatcher.annotation.Callback.class);
        if (callback != null) {
            filter = mergeFilters(filter, BuiltInFilters.callbackDataEquals(callback.value()));
        }
        ru.tardyon.botframework.dispatcher.annotation.CallbackPrefix callbackPrefix =
                method.getAnnotation(ru.tardyon.botframework.dispatcher.annotation.CallbackPrefix.class);
        if (callbackPrefix != null) {
            filter = mergeFilters(filter, BuiltInFilters.callbackDataStartsWith(callbackPrefix.value()));
        }
        return filter;
    }

    private Filter<?> composeClassFilter(UseFilters annotation) {
        return composeFilters(annotation);
    }

    private Filter<?> composeMethodFilter(UseFilters annotation) {
        return composeFilters(annotation);
    }

    @SuppressWarnings("unchecked")
    private Filter<?> composeFilters(UseFilters annotation) {
        if (annotation == null || annotation.value().length == 0) {
            return null;
        }
        Filter<?> merged = null;
        for (Class<? extends Filter<?>> type : annotation.value()) {
            Filter<?> raw = componentResolver.resolve(type);
            merged = mergeFilters(merged, adaptFilter(raw));
        }
        return merged;
    }

    private List<InnerMiddleware> resolveMiddlewares(UseMiddleware annotation) {
        if (annotation == null || annotation.value().length == 0) {
            return List.of();
        }
        ArrayList<InnerMiddleware> chain = new ArrayList<>(annotation.value().length);
        for (Class<? extends InnerMiddleware> type : annotation.value()) {
            chain.add(componentResolver.resolve(type));
        }
        return List.copyOf(chain);
    }

    @SuppressWarnings("unchecked")
    private static <TEvent> ContextualEventHandler<TEvent> wrapWithMethodMiddlewares(
            ContextualEventHandler<TEvent> delegate,
            List<InnerMiddleware> methodMiddlewares
    ) {
        return (event, context) -> MiddlewareChainExecutor.executeInner(
                        context,
                        methodMiddlewares,
                        () -> invokeHandler(delegate, event, context)
                )
                .thenCompose(result -> {
                    if (result.status() == DispatchStatus.FAILED) {
                        Throwable error = result.errorOpt().orElseGet(() -> new IllegalStateException("method middleware failed"));
                        return CompletableFuture.failedFuture(error);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    private static <TEvent> CompletionStage<DispatchResult> invokeHandler(
            ContextualEventHandler<TEvent> delegate,
            TEvent event,
            RuntimeContext context
    ) {
        try {
            CompletionStage<Void> stage = Objects.requireNonNull(delegate.handle(event, context), "handler stage");
            return stage.handle((ignored, throwable) -> throwable == null
                    ? DispatchResult.handled()
                    : DispatchResult.failed(unwrap(throwable)));
        } catch (Throwable throwable) {
            return CompletableFuture.completedFuture(DispatchResult.failed(unwrap(throwable)));
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof java.util.concurrent.CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
    }

    @SuppressWarnings("unchecked")
    private static <TEvent> Filter<TEvent> adaptFilter(Filter<?> raw) {
        Objects.requireNonNull(raw, "raw");
        Filter<Object> delegate = (Filter<Object>) raw;
        return new Filter<>() {
            @Override
            public CompletionStage<FilterResult> test(TEvent event) {
                try {
                    return delegate.test(event);
                } catch (ClassCastException ignored) {
                    return CompletableFuture.completedFuture(FilterResult.notMatched());
                }
            }

            @Override
            public CompletionStage<FilterResult> test(TEvent event, RuntimeContext context) {
                try {
                    return delegate.test(event, context);
                } catch (ClassCastException ignored) {
                    return CompletableFuture.completedFuture(FilterResult.notMatched());
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <TEvent> Filter<TEvent> mergeFilters(Filter<?> left, Filter<?> right) {
        if (left == null) {
            return (Filter<TEvent>) right;
        }
        if (right == null) {
            return (Filter<TEvent>) left;
        }
        return ((Filter<TEvent>) left).and((Filter<? super TEvent>) right);
    }

    private static int resolvePriority(Method method, EventType eventType) {
        if (eventType == EventType.CALLBACK) {
            boolean exact = method.isAnnotationPresent(ru.tardyon.botframework.dispatcher.annotation.Callback.class);
            boolean prefix = method.isAnnotationPresent(ru.tardyon.botframework.dispatcher.annotation.CallbackPrefix.class);
            if (exact && prefix) {
                return 300;
            }
            if (exact) {
                return 250;
            }
            if (prefix) {
                return 150;
            }
            return 100;
        }

        int priority = 100;
        Message message = method.getAnnotation(Message.class);
        if (message != null && !message.text().isBlank()) {
            priority = Math.max(priority, message.startsWith() ? 140 : 220);
        }
        if (method.isAnnotationPresent(Text.class)) {
            priority = Math.max(priority, 210);
        }
        if (method.isAnnotationPresent(Command.class)) {
            priority = Math.max(priority, 230);
        }
        if (method.isAnnotationPresent(State.class)) {
            priority = Math.max(priority, 160);
        }
        return priority;
    }

    private static String normalizeRouteName(String value) {
        String normalized = Objects.requireNonNull(value, "route name").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("@Route value must not be blank");
        }
        return normalized;
    }

    private enum EventType {
        MESSAGE,
        CALLBACK
    }

    private record Registration(EventType eventType, Filter<?> filter, ContextualEventHandler<?> handler, int priority) {
    }

    private record MethodRegistration(Method method, Registration registration) {
    }

    @FunctionalInterface
    public interface ComponentResolver {
        <T> T resolve(Class<T> type);

        static ComponentResolver reflectiveDefaults() {
            return new ComponentResolver() {
                @Override
                public <T> T resolve(Class<T> type) {
                    try {
                        var constructor = type.getDeclaredConstructor();
                        if (!constructor.canAccess(null)) {
                            constructor.setAccessible(true);
                        }
                        return constructor.newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Failed to instantiate " + type.getName(), e);
                    }
                }
            };
        }
    }
}
