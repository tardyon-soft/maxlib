package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.max.botframework.ingestion.UpdateHandlingStatus;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.CallbackId;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;

class DispatcherTest {

    @Test
    void includeRouterAddsRouterToGraph() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");

        dispatcher.includeRouter(router);

        assertEquals(1, dispatcher.routers().size());
        assertEquals("main", dispatcher.routers().getFirst().name());
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
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertSame(failure, result.errorOpt().orElseThrow());
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
        return new Update(
                new UpdateId("u-1"),
                UpdateType.MESSAGE,
                message(),
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
        return new Message(
                new MessageId("m-1"),
                new Chat(new ChatId("c-1"), ChatType.PRIVATE, "title", null, null),
                user(),
                "hello",
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                java.util.List.of(),
                java.util.List.of()
        );
    }

    private static User user() {
        return new User(new UserId("u-1"), "user", "First", "Last", "First Last", false, "ru");
    }
}
