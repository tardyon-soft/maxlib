package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
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

class MiddlewareChainExecutorTest {

    @Test
    void outerMiddlewareChainExecutesInDeterministicOrder() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        List<String> order = new ArrayList<>();

        OuterMiddleware first = (ctx, next) -> {
            order.add("outer-1-pre");
            return next.proceed().thenApply(result -> {
                order.add("outer-1-post");
                return result;
            });
        };
        OuterMiddleware second = (ctx, next) -> {
            order.add("outer-2-pre");
            return next.proceed().thenApply(result -> {
                order.add("outer-2-post");
                return result;
            });
        };

        DispatchResult result = MiddlewareChainExecutor.executeOuter(
                context,
                List.of(first, second),
                () -> {
                    order.add("terminal");
                    return CompletableFuture.completedFuture(DispatchResult.handled());
                }
        ).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(List.of("outer-1-pre", "outer-2-pre", "terminal", "outer-2-post", "outer-1-post"), order);
    }

    @Test
    void innerMiddlewareChainExecutesInDeterministicOrder() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        List<String> order = new ArrayList<>();

        InnerMiddleware first = (ctx, next) -> {
            order.add("inner-1-pre");
            return next.proceed().thenApply(result -> {
                order.add("inner-1-post");
                return result;
            });
        };
        InnerMiddleware second = (ctx, next) -> {
            order.add("inner-2-pre");
            return next.proceed().thenApply(result -> {
                order.add("inner-2-post");
                return result;
            });
        };

        DispatchResult result = MiddlewareChainExecutor.executeInner(
                context,
                List.of(first, second),
                () -> {
                    order.add("handler");
                    return CompletableFuture.completedFuture(DispatchResult.handled());
                }
        ).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(List.of("inner-1-pre", "inner-2-pre", "handler", "inner-2-post", "inner-1-post"), order);
    }

    @Test
    void middlewareCanEnrichRuntimeContext() {
        ContextKey<String> correlationId = ContextKey.of("correlationId", String.class);
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        AtomicBoolean terminalSawEnrichment = new AtomicBoolean(false);

        OuterMiddleware enrich = (ctx, next) -> {
            ctx.put(correlationId, "trace-1");
            return next.proceed();
        };

        DispatchResult result = MiddlewareChainExecutor.executeOuter(
                context,
                List.of(enrich),
                () -> {
                    terminalSawEnrichment.set("trace-1".equals(context.get(correlationId).orElse(null)));
                    return CompletableFuture.completedFuture(DispatchResult.handled());
                }
        ).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(terminalSawEnrichment.get());
    }

    @Test
    void middlewareCanShortCircuitChain() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        AtomicBoolean secondCalled = new AtomicBoolean(false);
        AtomicBoolean terminalCalled = new AtomicBoolean(false);

        OuterMiddleware first = (ctx, next) -> CompletableFuture.completedFuture(DispatchResult.ignored());
        OuterMiddleware second = (ctx, next) -> {
            secondCalled.set(true);
            return next.proceed();
        };

        DispatchResult result = MiddlewareChainExecutor.executeOuter(
                context,
                List.of(first, second),
                () -> {
                    terminalCalled.set(true);
                    return CompletableFuture.completedFuture(DispatchResult.handled());
                }
        ).toCompletableFuture().join();

        assertEquals(DispatchStatus.IGNORED, result.status());
        assertFalse(secondCalled.get());
        assertFalse(terminalCalled.get());
    }

    @Test
    void outerMiddlewareFailureIsWrappedWithOuterPhase() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        RuntimeException failure = new RuntimeException("outer failed");
        OuterMiddleware failing = (ctx, next) -> CompletableFuture.failedFuture(failure);

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> MiddlewareChainExecutor.executeOuter(
                        context,
                        List.of(failing),
                        () -> CompletableFuture.completedFuture(DispatchResult.handled())
                ).toCompletableFuture().join()
        );

        assertTrue(thrown.getCause() instanceof MiddlewareExecutionException);
        MiddlewareExecutionException wrapped = (MiddlewareExecutionException) thrown.getCause();
        assertEquals(MiddlewareExecutionException.Phase.OUTER, wrapped.phase());
        assertSame(failure, wrapped.rootCause());
    }

    @Test
    void innerMiddlewareFailureIsWrappedWithInnerPhase() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        RuntimeException failure = new RuntimeException("inner failed");
        InnerMiddleware failing = (ctx, next) -> CompletableFuture.failedFuture(failure);

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> MiddlewareChainExecutor.executeInner(
                        context,
                        List.of(failing),
                        () -> CompletableFuture.completedFuture(DispatchResult.handled())
                ).toCompletableFuture().join()
        );

        assertTrue(thrown.getCause() instanceof MiddlewareExecutionException);
        MiddlewareExecutionException wrapped = (MiddlewareExecutionException) thrown.getCause();
        assertEquals(MiddlewareExecutionException.Phase.INNER, wrapped.phase());
        assertSame(failure, wrapped.rootCause());
    }

    private static Update sampleUpdate() {
        return new Update(
                new UpdateId("u-mw-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-mw-1"),
                        new Chat(new ChatId("c-mw-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-mw-user"), "demo", "Demo", "User", "Demo User", false, "en"),
                        "hello",
                        Instant.parse("2026-03-12T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }
}
