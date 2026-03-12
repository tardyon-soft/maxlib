package ru.max.botframework.dispatcher;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Invokes method-based handlers by resolving method parameters through resolver pipeline.
 */
public interface HandlerInvoker {

    CompletionStage<Void> invoke(Object target, Method method, HandlerInvocationContext context);

    /**
     * Invokes one runtime handler using this invocation engine.
     *
     * <p>Reflective handlers are executed through parameter resolver pipeline.
     * Plain/contextual handlers keep lightweight direct invocation path.</p>
     */
    default <TEvent> CompletionStage<Void> invoke(
            EventHandler<TEvent> handler,
            TEvent event,
            RuntimeContext context
    ) {
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(context, "context");

        try {
            if (handler instanceof ReflectiveEventHandler<?> reflectiveHandler) {
                @SuppressWarnings("unchecked")
                ReflectiveEventHandler<TEvent> typed = (ReflectiveEventHandler<TEvent>) reflectiveHandler;
                return typed.invokeWith(this, event, context);
            }
            if (handler instanceof ContextualEventHandler<?> contextualHandler) {
                @SuppressWarnings("unchecked")
                ContextualEventHandler<TEvent> typed = (ContextualEventHandler<TEvent>) contextualHandler;
                return Objects.requireNonNull(typed.handle(event, context), "handler result");
            }
            return Objects.requireNonNull(handler.handle(event), "handler result");
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
    }
}
