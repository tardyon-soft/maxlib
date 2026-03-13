package ru.tardyon.botframework.examples.sprint9;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
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
 * Combined runtime example: filters + middleware + DI parameter resolution.
 */
public final class FiltersMiddlewareDiExample {

    public static void main(String[] args) throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.registerService(OrderService.class, new OrderService());

        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment("requestId", "req-1");
            return next.proceed();
        });

        Router router = new Router("filters-middleware-di");
        router.innerMiddleware((ctx, next) -> {
            ctx.putEnrichment("attempt", 1);
            return next.proceed();
        });

        Handler handlers = new Handler();
        Method onPay = Handler.class.getDeclaredMethod(
                "onPay",
                Message.class,
                String.class,
                Integer.class,
                OrderService.class
        );

        router.message(BuiltInFilters.textStartsWith("pay:"), handlers, onPay);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(sampleUpdate("pay:42"))
                .toCompletableFuture()
                .join();

        if (result.status() != DispatchStatus.HANDLED) {
            throw new IllegalStateException("Expected HANDLED, got: " + result.status());
        }
    }

    public static final class Handler {
        @SuppressWarnings("unused")
        public CompletableFuture<Void> onPay(
                Message message,
                String suffix,
                Integer attempt,
                OrderService service
        ) {
            System.out.println("message=" + message.text());
            System.out.println("suffix=" + suffix + ", attempt=" + attempt);
            System.out.println("service=" + service.describe(suffix));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static final class OrderService {
        public String describe(String orderId) {
            return "order-" + orderId;
        }
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-s9-fmd-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-s9-fmd-1"),
                        new Chat(new ChatId("c-s9-fmd-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-s9-fmd-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
