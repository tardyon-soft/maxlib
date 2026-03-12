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
import ru.max.botframework.ingestion.UpdateSink;
import ru.max.botframework.model.Update;

/**
 * Root runtime orchestrator that owns root routing graph and dispatch entrypoint.
 *
 * <p>This type is transport-agnostic and integrates with ingestion layer through
 * {@link UpdateConsumer}.</p>
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

    /**
     * Includes root router in dispatcher graph.
     *
     * <p>Only routers without parent can be included as roots.</p>
     */
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

    /**
     * Includes several root routers preserving include order.
     */
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
     * Backward-compatible ingestion adapter for APIs that still require {@link UpdateSink}.
     */
    @Deprecated(forRemoval = false)
    public UpdateSink asUpdateSink() {
        return this::handle;
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

    /**
     * Ingestion boundary adapter for runtime dispatch.
     */
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
        return notifyObserver(router.updates(), update, router, update)
                .thenCompose(genericResult -> {
                    if (genericResult.status() != DispatchStatus.IGNORED) {
                        return CompletableFuture.completedFuture(genericResult);
                    }
                    UpdateEventResolution resolution;
                    try {
                        resolution = eventResolver.resolve(update);
                    } catch (Throwable throwable) {
                        return handleFailure(
                                router,
                                update,
                                throwable,
                                RuntimeDispatchErrorType.EVENT_MAPPING_FAILURE
                        );
                    }
                    return notifyResolvedObserver(router, update, resolution);
                });
    }

    private <TEvent> CompletionStage<DispatchResult> notifyObserver(
            EventObserver<TEvent> observer,
            TEvent event,
            Router router,
            Update update
    ) {
        CompletionStage<HandlerExecutionResult> stage;
        try {
            stage = observer.notify(event);
        } catch (Throwable throwable) {
            return handleFailure(router, update, throwable, RuntimeDispatchErrorType.OBSERVER_EXECUTION_FAILURE);
        }
        return stage.handle((result, throwable) -> new ObserverNotificationOutcome(result, throwable))
                .thenCompose(outcome -> {
                    if (outcome.throwable() != null) {
                        return handleFailure(
                                router,
                                update,
                                unwrap(outcome.throwable()),
                                RuntimeDispatchErrorType.OBSERVER_EXECUTION_FAILURE
                        );
                    }
                    HandlerExecutionResult result = outcome.result();
                    if (result.status() == HandlerExecutionStatus.HANDLED) {
                        return CompletableFuture.completedFuture(DispatchResult.handled(result.enrichment()));
                    }
                    if (result.status() == HandlerExecutionStatus.FAILED) {
                        Throwable failure = result.errorOpt().orElseGet(() -> new IllegalStateException("handler failed without error"));
                        return handleFailure(router, update, failure, RuntimeDispatchErrorType.HANDLER_FAILURE);
                    }
                    return CompletableFuture.completedFuture(DispatchResult.ignored());
                });
    }

    private CompletionStage<DispatchResult> notifyResolvedObserver(
            Router router,
            Update update,
            UpdateEventResolution resolution
    ) {
        if (resolution.eventType() == ResolvedUpdateEventType.MESSAGE) {
            if (update.message() == null) {
                return CompletableFuture.completedFuture(DispatchResult.ignored());
            }
            return notifyObserver(router.messages(), update.message(), router, update);
        } else if (resolution.eventType() == ResolvedUpdateEventType.CALLBACK) {
            if (update.callback() == null) {
                return CompletableFuture.completedFuture(DispatchResult.ignored());
            }
            return notifyObserver(router.callbacks(), update.callback(), router, update);
        } else {
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        }
    }

    private CompletionStage<DispatchResult> handleFailure(
            Router router,
            Update update,
            Throwable error,
            RuntimeDispatchErrorType type
    ) {
        ErrorEvent event = new ErrorEvent(update, error, type);
        CompletionStage<HandlerExecutionResult> stage;
        try {
            stage = router.errors().notify(event);
        } catch (Throwable throwable) {
            error.addSuppressed(throwable);
            return CompletableFuture.completedFuture(DispatchResult.failed(error));
        }
        return stage
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        error.addSuppressed(unwrap(throwable));
                    } else if (result.status() == HandlerExecutionStatus.FAILED) {
                        result.errorOpt().ifPresent(error::addSuppressed);
                    }
                    return DispatchResult.failed(error);
                });
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private record ObserverNotificationOutcome(HandlerExecutionResult result, Throwable throwable) {
    }
}
