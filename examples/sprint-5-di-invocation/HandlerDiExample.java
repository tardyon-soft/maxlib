package ru.max.botframework.examples.sprint5;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.dispatcher.BuiltInFilters;
import ru.max.botframework.dispatcher.DispatchResult;
import ru.max.botframework.dispatcher.Dispatcher;
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

public final class HandlerDiExample {

    public static void main(String[] args) throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("di");
        ExampleHandlers handlers = new ExampleHandlers();

        // 4) shared application service
        dispatcher.registerService(OrderService.class, new OrderService());

        // 3) middleware-produced enrichment data
        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment("attempt", 1);
            return next.proceed();
        });

        // 1) core runtime/update parameters
        Method coreMethod = ExampleHandlers.class.getDeclaredMethod(
                "onCore",
                Message.class,
                Update.class,
                User.class,
                Chat.class
        );
        router.message(handlers, coreMethod);

        // 2) filter-produced data (String suffix from BuiltInFilters.textStartsWith("pay:"))
        Method filterMethod = ExampleHandlers.class.getDeclaredMethod("onFilterData", Message.class, String.class);
        router.message(BuiltInFilters.textStartsWith("pay:"), handlers, filterMethod);

        // 3 + 4 + 5) combined pipeline: filter + middleware + shared service + RuntimeContext
        Method combinedMethod = ExampleHandlers.class.getDeclaredMethod(
                "onCombined",
                Message.class,
                String.class,
                Integer.class,
                OrderService.class,
                ru.max.botframework.dispatcher.RuntimeContext.class
        );
        router.message(BuiltInFilters.textStartsWith("pay:"), handlers, combinedMethod);

        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(sampleUpdate("pay:42"))
                .toCompletableFuture()
                .join();

        System.out.println("Dispatch status: " + result.status());
    }

    public static final class ExampleHandlers {

        public CompletableFuture<Void> onCore(Message message, Update update, User user, Chat chat) {
            System.out.println("Core params => text=" + message.text()
                    + ", updateId=" + update.updateId().value()
                    + ", user=" + user.id().value()
                    + ", chat=" + chat.id().value());
            return CompletableFuture.completedFuture(null);
        }

        public CompletableFuture<Void> onFilterData(Message message, String suffix) {
            System.out.println("Filter data => text=" + message.text() + ", suffix=" + suffix);
            return CompletableFuture.completedFuture(null);
        }

        public CompletableFuture<Void> onCombined(
                Message message,
                String suffix,
                Integer attempt,
                OrderService orderService,
                ru.max.botframework.dispatcher.RuntimeContext context
        ) {
            System.out.println("Combined => text=" + message.text()
                    + ", suffix=" + suffix
                    + ", attempt=" + attempt
                    + ", serviceResult=" + orderService.describe(suffix)
                    + ", enrichment=" + context.enrichment());
            return CompletableFuture.completedFuture(null);
        }
    }

    public static final class OrderService {
        public String describe(String id) {
            return "order-" + id;
        }
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-s5-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-s5-1"),
                        new Chat(new ChatId("c-s5-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-s5-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        text,
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
