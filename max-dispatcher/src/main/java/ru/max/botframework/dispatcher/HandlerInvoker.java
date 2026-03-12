package ru.max.botframework.dispatcher;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

/**
 * Invokes method-based handlers by resolving method parameters through resolver pipeline.
 */
public interface HandlerInvoker {

    CompletionStage<Void> invoke(Object target, Method method, HandlerInvocationContext context);
}
