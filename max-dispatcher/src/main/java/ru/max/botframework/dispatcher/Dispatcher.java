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
    private final Map<RuntimeDataKey<?>, Object> applicationData = new java.util.concurrent.ConcurrentHashMap<>();
    private final UpdateEventResolver eventResolver;
    private final HandlerInvoker handlerInvoker;

    public Dispatcher() {
        this(new DefaultUpdateEventResolver(), DefaultHandlerInvoker.withDefaults());
    }

    public Dispatcher(UpdateEventResolver eventResolver) {
        this(eventResolver, DefaultHandlerInvoker.withDefaults());
    }

    public Dispatcher(HandlerInvoker handlerInvoker) {
        this(new DefaultUpdateEventResolver(), handlerInvoker);
    }

    public Dispatcher(UpdateEventResolver eventResolver, HandlerInvoker handlerInvoker) {
        this.eventResolver = Objects.requireNonNull(eventResolver, "eventResolver");
        this.handlerInvoker = Objects.requireNonNull(handlerInvoker, "handlerInvoker");
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
     * Registers shared application data for parameter resolution.
     *
     * <p>Only {@link RuntimeDataScope#APPLICATION} keys are allowed.
     * Dispatcher keeps references as-is and does not manage lifecycle of registered objects.</p>
     */
    public <T> Dispatcher registerApplicationData(RuntimeDataKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (key.scope() != RuntimeDataScope.APPLICATION) {
            throw new IllegalArgumentException("only APPLICATION scope keys can be registered on dispatcher");
        }
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match runtime data key type");
        }
        for (RuntimeDataKey<?> existingKey : applicationData.keySet()) {
            if (existingKey.name().equals(key.name()) && !existingKey.type().equals(key.type())) {
                throw new IllegalArgumentException(
                        "application data key '%s' is already registered with type %s"
                                .formatted(key.name(), existingKey.type().getName())
                );
            }
        }
        Object existing = applicationData.putIfAbsent(key, value);
        if (existing != null && !Objects.equals(existing, value)) {
            throw new RuntimeDataConflictException(key.name(), existing, value, key.scope());
        }
        return this;
    }

    /**
     * Registers shared service by type.
     *
     * <p>This is a typed convenience wrapper over {@link #registerApplicationData(RuntimeDataKey, Object)}.</p>
     */
    public <T> Dispatcher registerService(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        return registerApplicationData(RuntimeDataKey.application("service:" + type.getName(), type), service);
    }

    /**
     * Immutable snapshot of registered application data.
     */
    public Map<RuntimeDataKey<?>, Object> applicationData() {
        return Map.copyOf(applicationData);
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
        injectApplicationData(context);
        return MiddlewareChainExecutor.executeOuter(
                context,
                outerMiddlewares,
                () -> dispatchFromRoots(update, context, 0)
        ).handle((result, throwable) -> new FeedUpdateOutcome(result, throwable))
                .thenCompose(outcome -> {
                    if (outcome.throwable() == null) {
                        return CompletableFuture.completedFuture(outcome.result());
                    }
                    FailureClassification classification = classifyFailure(unwrap(outcome.throwable()));
                    if (classification.type() == RuntimeDispatchErrorType.OUTER_MIDDLEWARE_FAILURE) {
                        return handleGlobalFailure(update, classification.error(), classification.type());
                    }
                    return CompletableFuture.completedFuture(DispatchResult.failed(classification.error()));
                });
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
                        try {
                            mergeFilterEnrichment(context, result.enrichment());
                        } catch (Throwable throwable) {
                            FailureClassification classification = classifyFailure(throwable);
                            return handleFailure(
                                    router,
                                    update,
                                    classification.error(),
                                    classification.type()
                            );
                        }
                        return CompletableFuture.completedFuture(DispatchResult.handled(context.enrichment()));
                    }
                    if (result.status() == HandlerExecutionStatus.FAILED) {
                        Throwable failure = result.errorOpt().orElseGet(() -> new IllegalStateException("handler failed without error"));
                        FailureClassification classification = classifyFailure(failure);
                        return handleFailure(router, update, classification.error(), classification.type());
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
        mergeFilterEnrichment(context, enrichment);
        return MiddlewareChainExecutor.executeInner(
                        context,
                        router.innerMiddlewares(),
                        () -> invokeHandler(handlerInvoker, handler, event, context, enrichment)
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
            HandlerInvoker handlerInvoker,
            EventHandler<TEvent> handler,
            TEvent event,
            RuntimeContext context,
            Map<String, Object> enrichment
    ) {
        try {
            CompletionStage<Void> stage = Objects.requireNonNull(
                    handlerInvoker.invoke(handler, event, context),
                    "handler result"
            );
            return stage.handle((ignored, throwable) -> throwable == null
                    ? DispatchResult.handled(enrichment)
                    : DispatchResult.failed(HandlerInvocationException.wrap(unwrap(throwable))));
        } catch (Throwable throwable) {
            return CompletableFuture.completedFuture(DispatchResult.failed(HandlerInvocationException.wrap(throwable)));
        }
    }

    private static void mergeFilterEnrichment(RuntimeContext context, Map<String, Object> enrichment) {
        if (enrichment == null || enrichment.isEmpty()) {
            return;
        }
        context.mergeFilterEnrichment(enrichment);
    }

    private void injectApplicationData(RuntimeContext context) {
        for (Map.Entry<RuntimeDataKey<?>, Object> entry : applicationData.entrySet()) {
            putApplicationData(context, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putApplicationData(RuntimeContext context, RuntimeDataKey<?> key, Object value) {
        context.putData((RuntimeDataKey) key, value);
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

    private CompletionStage<DispatchResult> handleGlobalFailure(
            Update update,
            Throwable error,
            RuntimeDispatchErrorType type
    ) {
        if (rootRouters.isEmpty()) {
            return CompletableFuture.completedFuture(DispatchResult.failed(error));
        }
        Router router = rootRouters.stream()
                .filter(candidate -> !candidate.errors().handlers().isEmpty())
                .findFirst()
                .orElse(rootRouters.getFirst());
        return handleFailure(router, update, error, type);
    }

    private static FailureClassification classifyFailure(Throwable failure) {
        Throwable unwrapped = unwrap(failure);
        if (unwrapped instanceof FilterExecutionException filterFailure) {
            return new FailureClassification(
                    filterFailure.rootCause(),
                    RuntimeDispatchErrorType.FILTER_FAILURE
            );
        }
        if (unwrapped instanceof MiddlewareExecutionException middlewareFailure) {
            RuntimeDispatchErrorType type = middlewareFailure.phase() == MiddlewareExecutionException.Phase.OUTER
                    ? RuntimeDispatchErrorType.OUTER_MIDDLEWARE_FAILURE
                    : RuntimeDispatchErrorType.INNER_MIDDLEWARE_FAILURE;
            return new FailureClassification(middlewareFailure.rootCause(), type);
        }
        if (unwrapped instanceof HandlerInvocationException handlerFailure) {
            Throwable root = handlerFailure.rootCause();
            if (root instanceof ParameterResolutionException) {
                return new FailureClassification(root, RuntimeDispatchErrorType.PARAMETER_RESOLUTION_FAILURE);
            }
            if (root instanceof ReflectiveInvocationException) {
                return new FailureClassification(root, RuntimeDispatchErrorType.INVOCATION_FAILURE);
            }
            return new FailureClassification(
                    root,
                    RuntimeDispatchErrorType.HANDLER_FAILURE
            );
        }
        if (unwrapped instanceof EnrichmentConflictException enrichmentConflict) {
            return new FailureClassification(
                    enrichmentConflict,
                    RuntimeDispatchErrorType.ENRICHMENT_FAILURE
            );
        }
        return new FailureClassification(unwrapped, RuntimeDispatchErrorType.HANDLER_FAILURE);
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private record ObserverNotificationOutcome(HandlerExecutionResult result, Throwable throwable) {
    }

    private record FeedUpdateOutcome(DispatchResult result, Throwable throwable) {
    }

    private record FailureClassification(Throwable error, RuntimeDispatchErrorType type) {
    }
}
