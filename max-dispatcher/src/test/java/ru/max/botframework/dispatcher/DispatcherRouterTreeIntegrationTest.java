package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
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

class DispatcherRouterTreeIntegrationTest {

    @Test
    void routerTreeDispatchHandlesMessageInNestedRouter() {
        Router root = new Router("root");
        Router feature = new Router("feature");
        Router nested = new Router("nested");
        AtomicInteger nestedHandled = new AtomicInteger();

        nested.message(message -> {
            nestedHandled.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        feature.includeRouter(nested);
        root.includeRouter(feature);

        Dispatcher dispatcher = new Dispatcher().includeRouter(root);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, nestedHandled.get());
    }

    @Test
    void routerTreeDispatchRespectsFirstHandledAcrossNestedRouters() {
        Router root = new Router("root");
        Router firstChild = new Router("firstChild");
        Router secondChild = new Router("secondChild");
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();

        firstChild.callback(callback -> {
            firstCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        secondChild.callback(callback -> {
            secondCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });

        root.includeRouter(firstChild);
        root.includeRouter(secondChild);
        Dispatcher dispatcher = new Dispatcher().includeRouter(root);

        DispatchResult result = dispatcher.feedUpdate(callbackUpdate()).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, firstCalls.get());
        assertEquals(0, secondCalls.get());
    }

    private static Update messageUpdate() {
        return new Update(
                new UpdateId("u-tree-message"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-tree-1"),
                        new Chat(new ChatId("c-tree-1"), ChatType.PRIVATE, "title", null, null),
                        user(),
                        "hello",
                        Instant.parse("2026-03-12T00:00:00Z"),
                        null,
                        java.util.List.of(),
                        java.util.List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Update callbackUpdate() {
        return new Update(
                new UpdateId("u-tree-callback"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-tree-1"),
                        "pay:1",
                        user(),
                        messageForCallback(),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }

    private static Message messageForCallback() {
        return new Message(
                new MessageId("m-tree-2"),
                new Chat(new ChatId("c-tree-1"), ChatType.PRIVATE, "title", null, null),
                user(),
                "source",
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                java.util.List.of(),
                java.util.List.of()
        );
    }

    private static User user() {
        return new User(new UserId("u-tree-1"), "user", "First", "Last", "First Last", false, "ru");
    }
}

