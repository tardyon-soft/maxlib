package ru.max.botframework.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import ru.max.botframework.ingestion.UpdateConsumer;
import ru.max.botframework.ingestion.UpdateHandlingResult;
import ru.max.botframework.model.Update;

/**
 * Root runtime orchestrator that owns root routing graph and dispatch entrypoint.
 */
public final class Dispatcher implements UpdateConsumer {
    private final List<Router> rootRouters = new ArrayList<>();
    private final UpdateEventResolver eventResolver;

    public Dispatcher() {
        this(new DefaultUpdateEventResolver());
    }

    public Dispatcher(UpdateEventResolver eventResolver) {
        this.eventResolver = Objects.requireNonNull(eventResolver, "eventResolver");
    }

    public Dispatcher includeRouter(Router router) {
        Router candidate = Objects.requireNonNull(router, "router");
        if (candidate.parent().isPresent()) {
            throw new IllegalStateException("only root routers can be included in dispatcher");
        }
        if (rootRouters.contains(candidate)) {
            throw new IllegalStateException("router already included in dispatcher");
        }
        rootRouters.add(candidate);
        return this;
    }

    public Dispatcher includeRouters(Router... routers) {
        Objects.requireNonNull(routers, "routers");
        for (Router router : routers) {
            includeRouter(router);
        }
        return this;
    }

    public List<Router> routers() {
        return Collections.unmodifiableList(rootRouters);
    }

    /**
     * Feeds one normalized update into dispatcher runtime flow.
     */
    public CompletionStage<DispatchResult> feedUpdate(Update update) {
        Objects.requireNonNull(update, "update");
        return dispatchFromRoots(update, 0);
    }

    /**
     * @deprecated use {@link #feedUpdate(Update)} as primary runtime entrypoint.
     */
    @Deprecated(forRemoval = false)
    public CompletionStage<DispatchResult> dispatch(Update update) {
        return feedUpdate(update);
    }

    @Override
    public CompletionStage<UpdateHandlingResult> handle(Update update) {
        return feedUpdate(update).handle((result, throwable) -> {
            if (throwable != null) {
                return UpdateHandlingResult.failure(unwrap(throwable));
            }
            return result.status() == DispatchStatus.FAILED
                    ? UpdateHandlingResult.failure(result.errorOpt().orElseGet(() -> new IllegalStateException("dispatch failed")))
                    : UpdateHandlingResult.success();
        });
    }

    private CompletionStage<DispatchResult> dispatchFromRoots(Update update, int rootIndex) {
        if (rootIndex >= rootRouters.size()) {
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        }
        Router root = rootRouters.get(rootIndex);
        return dispatchRouterTree(root, update).thenCompose(result -> result.status() == DispatchStatus.IGNORED
                ? dispatchFromRoots(update, rootIndex + 1)
                : CompletableFuture.completedFuture(result));
    }

    private CompletionStage<DispatchResult> dispatchRouterTree(Router root, Update update) {
        List<Router> routers = root.traversalOrder();
        return dispatchInOrder(routers, update, 0);
    }

    private CompletionStage<DispatchResult> dispatchInOrder(List<Router> routers, Update update, int index) {
        if (index >= routers.size()) {
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        }
        Router router = routers.get(index);
        return notifyRouter(router, update).thenCompose(result -> result.status() == DispatchStatus.IGNORED
                ? dispatchInOrder(routers, update, index + 1)
                : CompletableFuture.completedFuture(result));
    }

    private CompletionStage<DispatchResult> notifyRouter(Router router, Update update) {
        return router.updates().notify(update)
                .thenCompose(result -> {
                    if (result.status() == HandlerExecutionStatus.HANDLED) {
                        return CompletableFuture.completedFuture(DispatchResult.handled());
                    }
                    if (result.status() == HandlerExecutionStatus.FAILED) {
                        return handleFailure(router, update, result.errorOpt().orElseThrow());
                    }
                    UpdateEventResolution resolution = eventResolver.resolve(update);
                    return notifyResolvedObserver(router, update, resolution);
                });
    }

    private CompletionStage<DispatchResult> notifyResolvedObserver(
            Router router,
            Update update,
            UpdateEventResolution resolution
    ) {
        CompletionStage<HandlerExecutionResult> stage;
        if (resolution.eventType() == ResolvedUpdateEventType.MESSAGE) {
            if (update.message() == null) {
                return CompletableFuture.completedFuture(DispatchResult.ignored());
            }
            stage = router.messages().notify(update.message());
        } else if (resolution.eventType() == ResolvedUpdateEventType.CALLBACK) {
            if (update.callback() == null) {
                return CompletableFuture.completedFuture(DispatchResult.ignored());
            }
            stage = router.callbacks().notify(update.callback());
        } else {
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        }

        return stage.thenCompose(result -> {
            if (result.status() == HandlerExecutionStatus.HANDLED) {
                return CompletableFuture.completedFuture(DispatchResult.handled());
            }
            if (result.status() == HandlerExecutionStatus.FAILED) {
                return handleFailure(router, update, result.errorOpt().orElseThrow());
            }
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        });
    }

    private CompletionStage<DispatchResult> handleFailure(Router router, Update update, Throwable error) {
        ErrorEvent event = new ErrorEvent(update, error);
        return router.errors().notify(event)
                .handle((ignored, throwable) -> DispatchResult.failed(error));
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }
}
