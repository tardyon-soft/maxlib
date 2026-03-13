package ru.tardyon.botframework.examples.sprint9;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
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

/**
 * Router tree composition example.
 */
public final class RouterCompositionExample {

    public static void main(String[] args) {
        Router root = new Router("root");
        Router orders = new Router("orders");
        Router admin = new Router("admin");

        orders.message((message, ctx) -> {
            System.out.println("orders handler: " + message.text());
            return CompletableFuture.completedFuture(null);
        });

        admin.message((message, ctx) -> {
            System.out.println("admin handler: " + message.text());
            return CompletableFuture.completedFuture(null);
        });

        root.includeRouter(orders);
        root.includeRouter(admin);

        Dispatcher dispatcher = new Dispatcher().includeRouter(root);

        DispatchResult result = dispatcher.feedUpdate(sampleUpdate("/orders list"))
                .toCompletableFuture()
                .join();

        System.out.println("Dispatch status: " + result.status());
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-s9-router-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-s9-router-1"),
                        new Chat(new ChatId("c-s9-router-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-s9-router-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        text,
                        Instant.parse("2026-03-13T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-13T00:00:00Z")
        );
    }
}
