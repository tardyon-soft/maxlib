package ru.max.botframework.dispatcher;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Method-based contextual handler adapter backed by {@link HandlerInvoker}.
 *
 * @param <TEvent> event type
 */
public final class ReflectiveEventHandler<TEvent> implements ContextualEventHandler<TEvent> {
    private final Object target;
    private final Method method;
    private final HandlerInvoker invoker;

    private ReflectiveEventHandler(Object target, Method method, HandlerInvoker invoker) {
        this.target = Objects.requireNonNull(target, "target");
        this.method = Objects.requireNonNull(method, "method");
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    public static <TEvent> ReflectiveEventHandler<TEvent> of(Object target, Method method, HandlerInvoker invoker) {
        return new ReflectiveEventHandler<>(target, method, invoker);
    }

    @Override
    public CompletionStage<Void> handle(TEvent event, RuntimeContext context) {
        return invoker.invoke(target, method, new HandlerInvocationContext(event, context));
    }
}
