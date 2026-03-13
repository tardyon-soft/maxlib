package ru.tardyon.botframework.examples.sprint3;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
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

public final class DispatcherRouterExample {

    public static void main(String[] args) {
        Router root = new Router("root")
                .update(update -> {
                    System.out.println("Any update: " + update.updateId().value());
                    return CompletableFuture.completedFuture(null);
                })
                .message(message -> {
                    System.out.println("Message: " + message.text());
                    return CompletableFuture.completedFuture(null);
                })
                .callback(callback -> {
                    System.out.println("Callback: " + callback.data());
                    return CompletableFuture.completedFuture(null);
                })
                .error(error -> {
                    System.err.println("Runtime error type: " + error.type());
                    error.error().printStackTrace();
                    return CompletableFuture.completedFuture(null);
                });

        Router feature = new Router("feature")
                .message(message -> CompletableFuture.completedFuture(null));
        root.includeRouter(feature);

        Dispatcher dispatcher = new Dispatcher()
                .includeRouter(root);

        DispatchResult result = dispatcher.feedUpdate(sampleMessageUpdate())
                .toCompletableFuture()
                .join();
        System.out.println("Dispatch status: " + result.status());

        DispatchResult callbackResult = dispatcher.feedUpdate(sampleCallbackUpdate())
                .toCompletableFuture()
                .join();
        System.out.println("Dispatch status: " + callbackResult.status());
    }

    private static Update sampleMessageUpdate() {
        return new Update(
                new UpdateId("u-s3-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-s3-1"),
                        new Chat(new ChatId("c-s3-1"), ChatType.PRIVATE, "chat", null, null),
                        user(),
                        "Hello from message update",
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

    private static Update sampleCallbackUpdate() {
        return new Update(
                new UpdateId("u-s3-2"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-s3-1"),
                        "pay:1",
                        user(),
                        new Message(
                                new MessageId("m-s3-2"),
                                new Chat(new ChatId("c-s3-1"), ChatType.PRIVATE, "chat", null, null),
                                user(),
                                "Callback source message",
                                Instant.parse("2026-03-12T00:00:01Z"),
                                null,
                                java.util.List.of(),
                                java.util.List.of()
                        ),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }

    private static User user() {
        return new User(new UserId("u-s3-user"), "demo", "Demo", "User", "Demo User", false, "en");
    }
}

