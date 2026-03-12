package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
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

class DispatcherInvocationPipelineIntegrationTest {

    @Test
    void reflectiveInvocationResolvesMultipleParameters() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("orders");
        PaymentService service = suffix -> "payment:" + suffix;
        MultiParamProbe probe = new MultiParamProbe();
        Method method = MultiParamProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                Update.class,
                User.class,
                Chat.class,
                RuntimeContext.class,
                String.class,
                Integer.class,
                PaymentService.class
        );

        dispatcher.registerService(PaymentService.class, service);
        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment("attempt", 3);
            return next.proceed();
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:42")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("pay:42", probe.lastText);
        assertEquals("42", probe.lastSuffix);
        assertEquals(3, probe.lastAttempt);
        assertEquals("u-invoke-1", probe.lastUserId);
        assertEquals("c-invoke-1", probe.lastChatId);
        assertSame(service, probe.lastService);
        assertEquals("payment:42", probe.lastServiceResult);
    }

    @Test
    void reflectiveInvocationRunsInsideFullDispatchPipeline() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        List<String> order = new ArrayList<>();
        PipelineProbe probe = new PipelineProbe(order);
        Method method = PipelineProbe.class.getDeclaredMethod("handle", Message.class);

        dispatcher.outerMiddleware((ctx, next) -> {
            order.add("outer-pre");
            return next.proceed().thenApply(result -> {
                order.add("outer-post");
                return result;
            });
        });
        router.innerMiddleware((ctx, next) -> {
            order.add("inner-pre");
            return next.proceed().thenApply(result -> {
                order.add("inner-post");
                return result;
            });
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:5")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(List.of("outer-pre", "inner-pre", "handler", "inner-post", "outer-post"), order);
    }

    @Test
    void reflectiveInvocationUsesMixedResolutionSources() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("mix");
        OrderService service = id -> "order-" + id;
        MixedSourceProbe probe = new MixedSourceProbe();
        Method method = MixedSourceProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                String.class,
                Long.class,
                OrderService.class
        );

        dispatcher.registerApplicationData(RuntimeDataKey.application("service.order", OrderService.class), service);
        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment("requestId", 9001L);
            return next.proceed();
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:777")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("777", probe.paymentSuffix);
        assertEquals(9001L, probe.requestId);
        assertSame(service, probe.service);
        assertEquals("order-777", probe.serviceResult);
        assertEquals(1, probe.calls.get());
    }

    @Test
    void fullPipelineRoutesResolutionFailureToErrorObserver() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("failures");
        Method method = MissingServiceProbe.class.getDeclaredMethod("onMessage", Message.class, PaymentService.class);
        List<String> order = new ArrayList<>();
        AtomicInteger errorCalls = new AtomicInteger();

        dispatcher.outerMiddleware((ctx, next) -> {
            order.add("outer-pre");
            return next.proceed().whenComplete((ignored, throwable) -> order.add("outer-post"));
        });
        router.innerMiddleware((ctx, next) -> {
            order.add("inner-pre");
            return next.proceed().whenComplete((ignored, throwable) -> order.add("inner-post"));
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), new MissingServiceProbe(), method);
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertEquals(RuntimeDispatchErrorType.PARAMETER_RESOLUTION_FAILURE, error.type());
            assertTrue(error.error() instanceof MissingHandlerDependencyException);
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:10")).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertTrue(result.errorOpt().orElseThrow() instanceof MissingHandlerDependencyException);
        assertEquals(1, errorCalls.get());
        assertEquals(List.of("outer-pre", "inner-pre", "inner-post", "outer-post"), order);
    }

    private interface PaymentService {
        String map(String suffix);
    }

    private interface OrderService {
        String make(String id);
    }

    private static final class MultiParamProbe {
        private String lastText;
        private String lastSuffix;
        private int lastAttempt;
        private String lastUserId;
        private String lastChatId;
        private PaymentService lastService;
        private String lastServiceResult;

        public CompletableFuture<Void> onMessage(
                Message message,
                Update update,
                User user,
                Chat chat,
                RuntimeContext context,
                String suffix,
                Integer attempt,
                PaymentService service
        ) {
            this.lastText = message.text();
            this.lastSuffix = suffix;
            this.lastAttempt = attempt;
            this.lastUserId = user.id().value();
            this.lastChatId = chat.id().value();
            this.lastService = service;
            this.lastServiceResult = service.map(suffix);
            assertSame(update, context.update());
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class PipelineProbe {
        private final List<String> order;

        private PipelineProbe(List<String> order) {
            this.order = order;
        }

        public CompletableFuture<Void> handle(Message message) {
            order.add("handler");
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class MixedSourceProbe {
        private String paymentSuffix;
        private long requestId;
        private OrderService service;
        private String serviceResult;
        private final AtomicInteger calls = new AtomicInteger();

        public CompletableFuture<Void> onMessage(Message message, String suffix, Long requestId, OrderService service) {
            this.paymentSuffix = suffix;
            this.requestId = requestId;
            this.service = service;
            this.serviceResult = service.make(suffix);
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class MissingServiceProbe {
        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, PaymentService service) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static Update messageUpdate(String text) {
        return new Update(
                new UpdateId("u-invoke-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-invoke-1"),
                        new Chat(new ChatId("c-invoke-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-invoke-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
