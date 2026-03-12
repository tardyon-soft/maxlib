package ru.max.botframework.middleware;

import ru.max.botframework.model.Update;

/**
 * Interceptor for update processing pipeline.
 */
@FunctionalInterface
public interface Middleware {
    void handle(Update update, Runnable next);
}
