package ru.tardyon.botframework.screen;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.InnerMiddleware;
import ru.tardyon.botframework.dispatcher.MiddlewareNext;
import ru.tardyon.botframework.dispatcher.RuntimeContext;

/**
 * Optional middleware adapter for screen callback/text handling.
 */
public final class ScreenMiddleware implements InnerMiddleware {
    private final ScreenRegistry registry;

    public ScreenMiddleware(ScreenRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next) {
        return ScreenRouter.middlewareHandler(context, registry)
                .thenCompose(result -> result.isHandled() ? java.util.concurrent.CompletableFuture.completedFuture(result) : next.proceed());
    }
}
