package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.ingestion.UpdateHandlingStatus;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

class DispatcherTest {

    @Test
    void includeRouterAddsRouterToGraph() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");

        dispatcher.includeRouter(router);

        assertEquals(1, dispatcher.routers().size());
        assertEquals("main", dispatcher.routers().get(0).name());
    }

    @Test
    void includeRoutersAddsAllRouters() {
        Dispatcher dispatcher = new Dispatcher();
        Router first = new Router("first");
        Router second = new Router("second");

        dispatcher.includeRouters(first, second);

        assertEquals(2, dispatcher.routers().size());
        assertEquals("first", dispatcher.routers().get(0).name());
        assertEquals("second", dispatcher.routers().get(1).name());
    }

    @Test
    void includeRouterRejectsNestedRouterAsRoot() {
        Router parent = new Router("parent");
        Router child = new Router("child");
        parent.includeRouter(child);
        Dispatcher dispatcher = new Dispatcher();

        assertThrows(IllegalStateException.class, () -> dispatcher.includeRouter(child));
    }

    @Test
    void includeRouterRejectsDuplicateRootRouter() {
        Router root = new Router("root");
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.includeRouter(root);

        assertThrows(IllegalStateException.class, () -> dispatcher.includeRouter(root));
    }

    @Test
    void feedUpdateRoutesMessageUpdateToMessageObserver() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger messageHandled = new AtomicInteger();
        router.message(message -> {
            messageHandled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, messageHandled.get());
    }

    @Test
    void feedUpdateExecutesHandlerWhenFilterMatches() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger handled = new AtomicInteger();
        router.message(Filter.of(message -> "ping".equals(message.text())), message -> {
            handled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("ping")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, handled.get());
    }

    @Test
    void feedUpdateInvokesContextualHandlerWithRuntimeContext() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger handled = new AtomicInteger();
        ContextKey<String> traceKey = ContextKey.of("traceId", String.class);

        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment(traceKey, "trace-ctx-1");
            return next.proceed();
        });
        router.message((message, ctx) -> {
            handled.incrementAndGet();
            assertEquals("hello", message.text());
            assertEquals("trace-ctx-1", ctx.enrichmentValue(traceKey).orElse(null));
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, handled.get());
    }

    @Test
    void feedUpdateSkipsHandlerWhenFilterDoesNotMatch() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger handled = new AtomicInteger();
        router.message(Filter.of(message -> "ping".equals(message.text())), message -> {
            handled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pong")).toCompletableFuture().join();

        assertEquals(DispatchStatus.IGNORED, result.status());
        assertEquals(0, handled.get());
    }

    @Test
    void feedUpdateSelectsFirstMatchingHandlerAmongFilteredHandlers() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        router.message(Filter.of(message -> false), message -> {
            first.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        router.message(Filter.of(message -> message.text().startsWith("pay:")), message -> {
            second.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:1")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(0, first.get());
        assertEquals(1, second.get());
    }

    @Test
    void feedUpdatePreservesFilterEnrichmentInDispatchResult() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        router.message(BuiltInFilters.textStartsWith("pay:"), message -> CompletableFuture.completedFuture(null));
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:42")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("42", result.enrichment().get(BuiltInFilters.TEXT_SUFFIX_KEY));
    }

    @Test
    void feedUpdateRunsOuterAndInnerMiddlewareInExpectedOrder() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        List<String> order = new ArrayList<>();

        dispatcher.outerMiddleware((ctx, next) -> {
            order.add("outer-1-pre");
            return next.proceed().thenApply(result -> {
                order.add("outer-1-post");
                return result;
            });
        });
        dispatcher.outerMiddleware((ctx, next) -> {
            order.add("outer-2-pre");
            return next.proceed().thenApply(result -> {
                order.add("outer-2-post");
                return result;
            });
        });

        router.innerMiddleware((ctx, next) -> {
            order.add("inner-1-pre");
            return next.proceed().thenApply(result -> {
                order.add("inner-1-post");
                return result;
            });
        });
        router.innerMiddleware((ctx, next) -> {
            order.add("inner-2-pre");
            return next.proceed().thenApply(result -> {
                order.add("inner-2-post");
                return result;
            });
        });
        router.message(message -> {
            order.add("handler");
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(
                List.of(
                        "outer-1-pre", "outer-2-pre",
                        "inner-1-pre", "inner-2-pre",
                        "handler",
                        "inner-2-post", "inner-1-post",
                        "outer-2-post", "outer-1-post"
                ),
                order
        );
    }

    @Test
    void outerMiddlewareCanShortCircuitDispatchBeforeHandler() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger handled = new AtomicInteger();
        dispatcher.outerMiddleware((ctx, next) -> CompletableFuture.completedFuture(DispatchResult.ignored()));
        router.message(message -> {
            handled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.IGNORED, result.status());
        assertEquals(0, handled.get());
    }

    @Test
    void innerMiddlewareCanShortCircuitMatchedHandlerExecution() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger handled = new AtomicInteger();
        router.message(Filter.of(message -> true), message -> {
            handled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        router.innerMiddleware((ctx, next) -> CompletableFuture.completedFuture(DispatchResult.ignored()));
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.IGNORED, result.status());
        assertEquals(0, handled.get());
    }

    @Test
    void innerMiddlewareCanReadFilterEnrichmentFromRuntimeContext() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger handled = new AtomicInteger();
        ContextKey<String> suffixKey = ContextKey.of("suffix", String.class);

        router.message(BuiltInFilters.textStartsWith("pay:"), message -> {
            handled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        router.innerMiddleware((ctx, next) -> {
            ctx.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY)
                    .map(String::valueOf)
                    .ifPresent(value -> ctx.put(suffixKey, value));
            return next.proceed().thenApply(result -> {
                assertEquals("42", ctx.get(suffixKey).orElse(null));
                return result;
            });
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:42")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("42", result.enrichment().get(BuiltInFilters.TEXT_SUFFIX_KEY));
        assertEquals(1, handled.get());
    }

    @Test
    void contextualHandlerCanUseFilterAndMiddlewareEnrichment() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        ContextKey<String> serviceKey = ContextKey.of("serviceName", String.class);
        AtomicInteger handled = new AtomicInteger();

        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.put(serviceKey, "orders");
            ctx.putEnrichment("trace", "trace-500");
            return next.proceed();
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), (message, ctx) -> {
            handled.incrementAndGet();
            assertEquals("orders", ctx.get(serviceKey).orElse(null));
            assertEquals("500", ctx.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY, String.class).orElse(null));
            assertEquals("trace-500", ctx.enrichmentValue("trace", String.class).orElse(null));
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:500")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, handled.get());
        assertEquals("500", result.enrichment().get(BuiltInFilters.TEXT_SUFFIX_KEY));
    }

    @Test
    void runtimeDataIsVisibleDownstreamInPipeline() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        RuntimeDataKey<String> serviceKey = RuntimeDataKey.application("service.name", String.class);
        AtomicInteger handled = new AtomicInteger();

        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putData(serviceKey, "payments");
            return next.proceed();
        });
        router.message((message, ctx) -> {
            handled.incrementAndGet();
            assertEquals("payments", ctx.dataValue(serviceKey).orElse(null));
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, handled.get());
    }

    @Test
    void dispatcherInvokesReflectiveHandlerThroughInvoker() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        ReflectiveHandlerProbe probe = new ReflectiveHandlerProbe();
        Method method = ReflectiveHandlerProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                RuntimeContext.class
        );
        ReflectiveEventHandler<Message> handler = ReflectiveEventHandler.of(
                probe,
                method,
                DefaultHandlerInvoker.withDefaults()
        );
        router.message(handler);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("hello reflective")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("hello reflective", probe.lastText.get());
    }

    @Test
    void reflectiveHandlerResolvesFilterDerivedParameter() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        ReflectiveDerivedProbe probe = new ReflectiveDerivedProbe();
        Method method = ReflectiveDerivedProbe.class.getDeclaredMethod(
                "onPayment",
                Message.class,
                String.class
        );
        router.message(
                BuiltInFilters.textStartsWith("pay:"),
                ReflectiveEventHandler.of(probe, method, DefaultHandlerInvoker.withDefaults())
        );
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:777")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("777", probe.lastDerived.get());
    }

    @Test
    void reflectiveHandlerUsesFilterDataBeforeMiddlewareDataForSameType() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        ReflectiveDerivedProbe probe = new ReflectiveDerivedProbe();
        Method method = ReflectiveDerivedProbe.class.getDeclaredMethod(
                "onPayment",
                Message.class,
                String.class
        );
        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment("trace", "middleware-string");
            return next.proceed();
        });
        router.message(
                BuiltInFilters.textStartsWith("pay:"),
                ReflectiveEventHandler.of(probe, method, DefaultHandlerInvoker.withDefaults())
        );
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:888")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("888", probe.lastDerived.get());
    }

    @Test
    void middlewareAndFilterEnrichmentAreMergedAndVisibleDownstream() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        ContextKey<String> traceKey = ContextKey.of("traceId", String.class);
        AtomicInteger handled = new AtomicInteger();

        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment(traceKey, "trace-42");
            return next.proceed();
        });
        router.innerMiddleware((ctx, next) -> {
            assertEquals("trace-42", ctx.enrichmentValue(traceKey).orElse(null));
            assertEquals("7", ctx.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY, String.class).orElse(null));
            return next.proceed();
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), message -> {
            handled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:7")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("trace-42", result.enrichment().get("traceId"));
        assertEquals("7", result.enrichment().get(BuiltInFilters.TEXT_SUFFIX_KEY));
        assertEquals(1, handled.get());
    }

    @Test
    void conflictingMiddlewareAndFilterEnrichmentFailsDispatch() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger errorCalls = new AtomicInteger();
        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment(BuiltInFilters.TEXT_SUFFIX_KEY, "outer");
            return next.proceed();
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), message -> CompletableFuture.completedFuture(null));
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertEquals(RuntimeDispatchErrorType.ENRICHMENT_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdateWithText("pay:9")).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertTrue(result.errorOpt().orElseThrow() instanceof EnrichmentConflictException);
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateRoutesFilterFailureToErrorObserver() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger errorCalls = new AtomicInteger();
        RuntimeException filterFailure = new RuntimeException("filter failed");
        router.message(
                message -> CompletableFuture.completedFuture(FilterResult.failed(filterFailure)),
                message -> CompletableFuture.completedFuture(null)
        );
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertSame(filterFailure, error.error());
            assertEquals(RuntimeDispatchErrorType.FILTER_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertSame(filterFailure, result.errorOpt().orElseThrow());
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateRoutesInnerMiddlewareFailureToErrorObserver() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger errorCalls = new AtomicInteger();
        RuntimeException middlewareFailure = new RuntimeException("inner middleware failed");
        router.innerMiddleware((ctx, next) -> CompletableFuture.failedFuture(middlewareFailure));
        router.message(message -> CompletableFuture.completedFuture(null));
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertSame(middlewareFailure, error.error());
            assertEquals(RuntimeDispatchErrorType.INNER_MIDDLEWARE_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertSame(middlewareFailure, result.errorOpt().orElseThrow());
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateRoutesOuterMiddlewareFailureToErrorObserver() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger errorCalls = new AtomicInteger();
        RuntimeException middlewareFailure = new RuntimeException("outer middleware failed");

        dispatcher.outerMiddleware((ctx, next) -> CompletableFuture.failedFuture(middlewareFailure));
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertSame(middlewareFailure, error.error());
            assertEquals(RuntimeDispatchErrorType.OUTER_MIDDLEWARE_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertSame(middlewareFailure, result.errorOpt().orElseThrow());
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateUsesResolverFallbackForUnknownTypeWithMessagePayload() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger handled = new AtomicInteger();
        router.message(message -> {
            handled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        Update update = new Update(
                new UpdateId("u-fallback"),
                UpdateType.UNKNOWN,
                message(),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );

        DispatchResult result = dispatcher.feedUpdate(update).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, handled.get());
    }

    @Test
    void feedUpdateStopsAtFirstMatchedHandlerInSameObserver() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        router.message(message -> {
            firstCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        router.message(message -> {
            secondCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, firstCalls.get());
        assertEquals(0, secondCalls.get());
    }

    @Test
    void feedUpdatePropagatesToChildRoutersUntilHandled() {
        Dispatcher dispatcher = new Dispatcher();
        Router root = new Router("root");
        Router child = new Router("child");
        AtomicInteger childHandled = new AtomicInteger();

        child.message(message -> {
            childHandled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        root.includeRouter(child);
        dispatcher.includeRouter(root);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, childHandled.get());
    }

    @Test
    void feedUpdateStopsOnFirstHandledRootRouter() {
        Dispatcher dispatcher = new Dispatcher();
        Router first = new Router("first");
        Router second = new Router("second");
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();

        first.message(message -> {
            firstCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        second.message(message -> {
            secondCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });

        dispatcher.includeRouters(first, second);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, firstCalls.get());
        assertEquals(0, secondCalls.get());
    }

    @Test
    void feedUpdateReturnsIgnoredWhenNoObserversHandleUpdate() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.includeRouter(new Router("main"));

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.IGNORED, result.status());
    }

    @Test
    void feedUpdateReturnsIgnoredWhenNoHandlersAcrossRouterTree() {
        Dispatcher dispatcher = new Dispatcher();
        Router root = new Router("root");
        Router child = new Router("child");
        root.includeRouter(child);
        dispatcher.includeRouter(root);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.IGNORED, result.status());
    }

    @Test
    void feedUpdateReturnsIgnoredForUnsupportedUpdateType() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.includeRouter(new Router("main"));
        Update update = new Update(
                new UpdateId("u-unsupported"),
                UpdateType.CHAT_MEMBER,
                null,
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );

        DispatchResult result = dispatcher.feedUpdate(update).toCompletableFuture().join();

        assertEquals(DispatchStatus.IGNORED, result.status());
    }

    @Test
    void feedUpdateReturnsFailedAndNotifiesErrorObserver() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        RuntimeException failure = new RuntimeException("handler failed");
        AtomicInteger errorCalls = new AtomicInteger();
        router.message(message -> CompletableFuture.failedFuture(failure));
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertSame(failure, error.error());
            assertEquals(RuntimeDispatchErrorType.HANDLER_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertSame(failure, result.errorOpt().orElseThrow());
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateRoutesMissingDependencyToErrorObserverAsResolutionFailure() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        MissingDependencyProbe probe = new MissingDependencyProbe();
        Method method = MissingDependencyProbe.class.getDeclaredMethod("onMessage", Message.class, PaymentService.class);
        AtomicInteger errorCalls = new AtomicInteger();

        router.message(probe, method);
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertTrue(error.error() instanceof MissingHandlerDependencyException);
            assertEquals(RuntimeDispatchErrorType.PARAMETER_RESOLUTION_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertTrue(result.errorOpt().orElseThrow() instanceof MissingHandlerDependencyException);
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateRoutesAmbiguousResolutionToErrorObserverAsResolutionFailure() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        AmbiguousResolutionProbe probe = new AmbiguousResolutionProbe();
        Method method = AmbiguousResolutionProbe.class.getDeclaredMethod("onMessage", Message.class, String.class);
        AtomicInteger errorCalls = new AtomicInteger();

        dispatcher.registerApplicationData(RuntimeDataKey.application("app.one", String.class), "one");
        dispatcher.registerApplicationData(RuntimeDataKey.application("app.two", String.class), "two");
        router.message(probe, method);
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertTrue(error.error() instanceof ParameterResolutionException);
            ParameterResolutionException resolution = (ParameterResolutionException) error.error();
            assertEquals(ParameterResolutionException.Reason.AMBIGUOUS_RESOLUTION, resolution.reason());
            assertEquals(RuntimeDispatchErrorType.PARAMETER_RESOLUTION_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertTrue(result.errorOpt().orElseThrow() instanceof ParameterResolutionException);
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateRoutesReflectiveInfrastructureFailureToErrorObserver() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        ReflectiveInvalidReturnProbe probe = new ReflectiveInvalidReturnProbe();
        Method method = ReflectiveInvalidReturnProbe.class.getDeclaredMethod("onMessage", Message.class);
        AtomicInteger errorCalls = new AtomicInteger();

        router.message(probe, method);
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertTrue(error.error() instanceof ReflectiveInvocationException);
            assertEquals(RuntimeDispatchErrorType.INVOCATION_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertTrue(result.errorOpt().orElseThrow() instanceof ReflectiveInvocationException);
        assertEquals(1, errorCalls.get());
    }

    @Test
    void feedUpdateReturnsFailedWhenNoErrorObserverHandlerRegistered() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        RuntimeException failure = new RuntimeException("handler failed");
        router.message(message -> CompletableFuture.failedFuture(failure));
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertSame(failure, result.errorOpt().orElseThrow());
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    void feedUpdateAddsSuppressedWhenErrorObserverHandlerFails() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        RuntimeException originalFailure = new RuntimeException("handler failed");
        RuntimeException errorObserverFailure = new RuntimeException("error observer failed");

        router.message(message -> CompletableFuture.failedFuture(originalFailure));
        router.error(error -> CompletableFuture.failedFuture(errorObserverFailure));
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        Throwable failure = result.errorOpt().orElseThrow();
        assertSame(originalFailure, failure);
        assertEquals(1, failure.getSuppressed().length);
        assertSame(errorObserverFailure, failure.getSuppressed()[0]);
    }

    @Test
    void feedUpdateRoutesEventMappingFailureToErrorObserver() {
        UpdateEventResolver failingResolver = update -> {
            throw new IllegalStateException("resolver failed");
        };
        Dispatcher dispatcher = new Dispatcher(failingResolver);
        Router router = new Router("main");
        AtomicInteger errorCalls = new AtomicInteger();
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertEquals(RuntimeDispatchErrorType.EVENT_MAPPING_FAILURE, error.type());
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertEquals(1, errorCalls.get());
    }

    @Test
    void handleAdaptsDispatchResultToIngestionContract() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        router.callback(callback -> CompletableFuture.completedFuture(null));
        dispatcher.includeRouter(router);

        var handled = dispatcher.handle(callbackUpdate()).toCompletableFuture().join();
        var ignored = dispatcher.handle(messageUpdate()).toCompletableFuture().join();

        assertEquals(UpdateHandlingStatus.SUCCESS, handled.status());
        assertEquals(UpdateHandlingStatus.SUCCESS, ignored.status());
        assertTrue(handled.isSuccess());
        assertTrue(ignored.isSuccess());
    }

    private static Update messageUpdate() {
        return messageUpdateWithText("hello");
    }

    private static Update messageUpdateWithText(String text) {
        return new Update(
                new UpdateId("u-1"),
                UpdateType.MESSAGE,
                message(text),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Update callbackUpdate() {
        return new Update(
                new UpdateId("u-2"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-1"),
                        "action:1",
                        user(),
                        message(),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }

    private static Message message() {
        return message("hello");
    }

    private static Message message(String text) {
        return new Message(
                new MessageId("m-1"),
                new Chat(new ChatId("c-1"), ChatType.PRIVATE, "title", null, null),
                user(),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                java.util.List.of(),
                java.util.List.of()
        );
    }

    private static User user() {
        return new User(new UserId("u-1"), "user", "First", "Last", "First Last", false, "ru");
    }

    private static final class ReflectiveHandlerProbe {
        private final java.util.concurrent.atomic.AtomicReference<String> lastText = new java.util.concurrent.atomic.AtomicReference<>();

        public void onMessage(Message message, RuntimeContext context) {
            lastText.set(message.text());
            context.putEnrichment("reflective", "ok");
        }
    }

    private static final class ReflectiveDerivedProbe {
        private final java.util.concurrent.atomic.AtomicReference<String> lastDerived = new java.util.concurrent.atomic.AtomicReference<>();

        public void onPayment(Message message, String derivedValue) {
            lastDerived.set(derivedValue);
        }
    }

    private interface PaymentService {
    }

    private static final class MissingDependencyProbe {
        @SuppressWarnings("unused")
        public void onMessage(Message message, PaymentService service) {
        }
    }

    private static final class AmbiguousResolutionProbe {
        @SuppressWarnings("unused")
        public void onMessage(Message message, String anyString) {
        }
    }

    private static final class ReflectiveInvalidReturnProbe {
        @SuppressWarnings("unused")
        public String onMessage(Message message) {
            return message.text();
        }
    }
}
