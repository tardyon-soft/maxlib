package ru.tardyon.botframework.dispatcher;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Method-based contextual handler adapter backed by {@link HandlerInvoker}.
 *
 * @param <TEvent> event type
 */
public final class ReflectiveEventHandler<TEvent> implements ContextualEventHandler<TEvent> {
    private static final HandlerInvoker DEFAULT_INVOKER = DefaultHandlerInvoker.withDefaults();
    private final Object target;
    private final Method method;
    private final HandlerInvoker invokerOverride;

    private ReflectiveEventHandler(Object target, Method method, HandlerInvoker invokerOverride) {
        this.target = Objects.requireNonNull(target, "target");
        this.method = Objects.requireNonNull(method, "method");
        this.invokerOverride = invokerOverride;
    }

    /**
     * Creates reflective handler resolved by dispatcher-level invocation engine.
     */
    public static <TEvent> ReflectiveEventHandler<TEvent> of(Object target, Method method) {
        return new ReflectiveEventHandler<>(target, method, null);
    }

    /**
     * Creates reflective handler with explicit invoker override.
     */
    public static <TEvent> ReflectiveEventHandler<TEvent> of(Object target, Method method, HandlerInvoker invoker) {
        return new ReflectiveEventHandler<>(target, method, Objects.requireNonNull(invoker, "invoker"));
    }

    @Override
    public CompletionStage<Void> handle(TEvent event, RuntimeContext context) {
        HandlerInvoker invoker = invokerOverride == null ? DEFAULT_INVOKER : invokerOverride;
        return invoker.invoke(target, method, new HandlerInvocationContext(event, context));
    }

    CompletionStage<Void> invokeWith(HandlerInvoker fallbackInvoker, TEvent event, RuntimeContext context) {
        Objects.requireNonNull(fallbackInvoker, "fallbackInvoker");
        HandlerInvoker invoker = invokerOverride == null ? fallbackInvoker : invokerOverride;
        return invoker.invoke(target, method, new HandlerInvocationContext(event, context));
    }
}
