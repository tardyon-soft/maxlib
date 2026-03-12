package ru.max.botframework.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final List<OuterMiddleware> outerMiddlewares = new ArrayList<>();
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
     * Registers dispatcher-scoped outer middleware.
     */
    public Dispatcher outerMiddleware(OuterMiddleware middleware) {
        outerMiddlewares.add(Objects.requireNonNull(middleware, "middleware"));
        return this;
    }

    public List<OuterMiddleware> outerMiddlewares() {
        return Collections.unmodifiableList(outerMiddlewares);
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
        RuntimeContext context = new RuntimeContext(update);
        return MiddlewareChainExecutor.executeOuter(
                context,
                outerMiddlewares,
                () -> dispatchFromRoots(update, context, 0)
        );
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

    private CompletionStage<DispatchResult> dispatchFromRoots(Update update, RuntimeContext context, int rootIndex) {
        if (rootIndex >= rootRouters.size()) {
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        }
        Router root = rootRouters.get(rootIndex);
        return dispatchRouterTree(root, update, context).thenCompose(result -> result.status() == DispatchStatus.IGNORED
                ? dispatchFromRoots(update, context, rootIndex + 1)
                : CompletableFuture.completedFuture(result));
    }

    private CompletionStage<DispatchResult> dispatchRouterTree(Router root, Update update, RuntimeContext context) {
        List<Router> routers = root.traversalOrder();
        return dispatchInOrder(routers, update, context, 0);
    }

    private CompletionStage<DispatchResult> dispatchInOrder(
            List<Router> routers,
            Update update,
            RuntimeContext context,
            int index
    ) {
        if (index >= routers.size()) {
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        }
        Router router = routers.get(index);
        return notifyRouter(router, update, context).thenCompose(result -> result.status() == DispatchStatus.IGNORED
                ? dispatchInOrder(routers, update, context, index + 1)
                : CompletableFuture.completedFuture(result));
    }

    private CompletionStage<DispatchResult> notifyRouter(Router router, Update update, RuntimeContext context) {
        return notifyObserver(router.updates(), update, router, update, context)
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
                    return notifyResolvedObserver(router, update, resolution, context);
                });
    }

    private <TEvent> CompletionStage<DispatchResult> notifyObserver(
            EventObserver<TEvent> observer,
            TEvent event,
            Router router,
            Update update,
            RuntimeContext context
    ) {
        CompletionStage<HandlerExecutionResult> stage;
        try {
            stage = observer.notify(event, (handler, handlerEvent, enrichment) ->
                    executeHandlerWithInnerMiddleware(router, context, handler, handlerEvent, enrichment));
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
                        mergeEnrichment(context, result.enrichment());
                        return CompletableFuture.completedFuture(DispatchResult.handled(context.enrichment()));
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
            UpdateEventResolution resolution,
            RuntimeContext context
    ) {
        if (resolution.eventType() == ResolvedUpdateEventType.MESSAGE) {
            if (update.message() == null) {
                return CompletableFuture.completedFuture(DispatchResult.ignored());
            }
            return notifyObserver(router.messages(), update.message(), router, update, context);
        } else if (resolution.eventType() == ResolvedUpdateEventType.CALLBACK) {
            if (update.callback() == null) {
                return CompletableFuture.completedFuture(DispatchResult.ignored());
            }
            return notifyObserver(router.callbacks(), update.callback(), router, update, context);
        } else {
            return CompletableFuture.completedFuture(DispatchResult.ignored());
        }
    }

    private <TEvent> CompletionStage<HandlerExecutionResult> executeHandlerWithInnerMiddleware(
            Router router,
            RuntimeContext context,
            EventHandler<TEvent> handler,
            TEvent event,
            Map<String, Object> enrichment
    ) {
        mergeEnrichment(context, enrichment);
        return MiddlewareChainExecutor.executeInner(
                        context,
                        router.innerMiddlewares(),
                        () -> invokeHandler(handler, event, enrichment)
                )
                .handle((dispatchResult, throwable) -> {
                    if (throwable != null) {
                        return HandlerExecutionResult.failed(unwrap(throwable));
                    }
                    if (dispatchResult.status() == DispatchStatus.HANDLED) {
                        return HandlerExecutionResult.handled(dispatchResult.enrichment());
                    }
                    if (dispatchResult.status() == DispatchStatus.FAILED) {
                        return HandlerExecutionResult.failed(
                                dispatchResult.errorOpt().orElseGet(() -> new IllegalStateException("inner middleware failed"))
                        );
                    }
                    return HandlerExecutionResult.ignored();
                });
    }

    private static <TEvent> CompletionStage<DispatchResult> invokeHandler(
            EventHandler<TEvent> handler,
            TEvent event,
            Map<String, Object> enrichment
    ) {
        try {
            CompletionStage<Void> stage = Objects.requireNonNull(handler.handle(event), "handler result");
            return stage.handle((ignored, throwable) -> throwable == null
                    ? DispatchResult.handled(enrichment)
                    : DispatchResult.failed(unwrap(throwable)));
        } catch (Throwable throwable) {
            return CompletableFuture.completedFuture(DispatchResult.failed(throwable));
        }
    }

    private static void mergeEnrichment(RuntimeContext context, Map<String, Object> enrichment) {
        if (enrichment == null || enrichment.isEmpty()) {
            return;
        }
        context.putAllEnrichment(enrichment);
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
