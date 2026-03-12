package ru.max.botframework.examples.sprint4;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.dispatcher.BuiltInFilters;
import ru.max.botframework.dispatcher.ContextKey;
import ru.max.botframework.dispatcher.DispatchResult;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Filter;
import ru.max.botframework.dispatcher.Router;
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

public final class FiltersMiddlewareExample {

    private static final ContextKey<String> TRACE_ID = ContextKey.of("traceId", String.class);

    public static void main(String[] args) {
        Router root = new Router("root");
        Router payments = new Router("payments");

        // 1) Built-in filter registration.
        root.message(BuiltInFilters.command("start"), message -> {
            System.out.println("Start command received");
            return CompletableFuture.completedFuture(null);
        });

        // 2) Filter composition.
        Filter<Message> payFilter = BuiltInFilters.chatType(ChatType.PRIVATE)
                .and(BuiltInFilters.textStartsWith("pay:"));
        payments.message(payFilter, message -> {
            System.out.println("Payment payload: " + message.text());
            return CompletableFuture.completedFuture(null);
        });

        // 4) Inner middleware + 5) context enrichment read.
        payments.innerMiddleware((ctx, next) -> {
            String trace = ctx.enrichmentValue(TRACE_ID).orElse("trace-missing");
            String suffix = ctx.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY, String.class).orElse("n/a");
            System.out.println("Inner middleware trace=" + trace + " suffix=" + suffix);
            return next.proceed();
        });

        // 6) Router tree.
        root.includeRouter(payments);

        Dispatcher dispatcher = new Dispatcher()
                // 3) Outer middleware + 5) context enrichment write.
                .outerMiddleware((ctx, next) -> {
                    ctx.putEnrichment(TRACE_ID, "trace-" + ctx.update().updateId().value());
                    return next.proceed();
                })
                .includeRouter(root);

        DispatchResult result = dispatcher.feedUpdate(samplePayUpdate())
                .toCompletableFuture()
                .join();
        System.out.println("Dispatch status: " + result.status());
        System.out.println("Merged enrichment: " + result.enrichment());
    }

    private static Update samplePayUpdate() {
        return new Update(
                new UpdateId("u-s4-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-s4-1"),
                        new Chat(new ChatId("c-s4-1"), ChatType.PRIVATE, "chat", null, null),
                        user(),
                        "pay:42",
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

    private static User user() {
        return new User(new UserId("u-s4-user"), "demo", "Demo", "User", "Demo User", false, "en");
    }
}
