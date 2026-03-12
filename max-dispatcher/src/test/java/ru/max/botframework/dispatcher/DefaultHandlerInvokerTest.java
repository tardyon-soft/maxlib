package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
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

class DefaultHandlerInvokerTest {

    @Test
    void resolvesSingleParameter() throws Exception {
        ResolverRegistry registry = new ResolverRegistry().register(new EventParameterResolver());
        DefaultHandlerInvoker invoker = new DefaultHandlerInvoker(registry);
        HandlerSamples samples = new HandlerSamples();
        Method method = HandlerSamples.class.getDeclaredMethod("single", Message.class);
        Message message = message("hello");
        RuntimeContext context = new RuntimeContext(messageUpdate("hello"));

        invoker.invoke(samples, method, new HandlerInvocationContext(message, context))
                .toCompletableFuture()
                .join();

        assertSame(message, samples.lastMessage.get());
    }

    @Test
    void resolvesMultipleParameters() throws Exception {
        ResolverRegistry registry = new ResolverRegistry()
                .register(new RuntimeContextParameterResolver())
                .register(new UpdateParameterResolver())
                .register(new EventParameterResolver());
        DefaultHandlerInvoker invoker = new DefaultHandlerInvoker(registry);
        HandlerSamples samples = new HandlerSamples();
        Method method = HandlerSamples.class.getDeclaredMethod(
                "multiple",
                Message.class,
                RuntimeContext.class,
                Update.class
        );
        Update update = messageUpdate("ping");
        Message event = update.message();
        RuntimeContext context = new RuntimeContext(update);

        invoker.invoke(samples, method, new HandlerInvocationContext(event, context))
                .toCompletableFuture()
                .join();

        assertSame(event, samples.lastMessage.get());
        assertSame(context, samples.lastContext.get());
        assertSame(update, samples.lastUpdate.get());
    }

    @Test
    void resolverOrderingUsesFirstSupportingResolver() throws Exception {
        HandlerParameterResolver first = (parameter, ctx) ->
                parameter.type() == String.class
                        ? HandlerParameterResolution.resolved("first")
                        : HandlerParameterResolution.unsupported();
        HandlerParameterResolver second = (parameter, ctx) ->
                parameter.type() == String.class
                        ? HandlerParameterResolution.resolved("second")
                        : HandlerParameterResolution.unsupported();
        DefaultHandlerInvoker invoker = new DefaultHandlerInvoker(
                new ResolverRegistry().register(first).register(second)
        );
        HandlerSamples samples = new HandlerSamples();
        Method method = HandlerSamples.class.getDeclaredMethod("stringOnly", String.class);
        RuntimeContext context = new RuntimeContext(messageUpdate("ignored"));

        invoker.invoke(samples, method, new HandlerInvocationContext(message("ignored"), context))
                .toCompletableFuture()
                .join();

        assertEquals("first", samples.lastString.get());
    }

    @Test
    void unsupportedParameterProducesResolutionFailure() throws Exception {
        DefaultHandlerInvoker invoker = new DefaultHandlerInvoker(
                new ResolverRegistry().register(new EventParameterResolver())
        );
        HandlerSamples samples = new HandlerSamples();
        Method method = HandlerSamples.class.getDeclaredMethod("unsupported", Integer.class);
        RuntimeContext context = new RuntimeContext(messageUpdate("x"));

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> invoker.invoke(samples, method, new HandlerInvocationContext(message("x"), context))
                        .toCompletableFuture()
                        .join()
        );

        assertTrue(thrown.getCause() instanceof UnsupportedHandlerParameterException);
    }

    private static final class HandlerSamples {
        private final AtomicReference<Message> lastMessage = new AtomicReference<>();
        private final AtomicReference<RuntimeContext> lastContext = new AtomicReference<>();
        private final AtomicReference<Update> lastUpdate = new AtomicReference<>();
        private final AtomicReference<String> lastString = new AtomicReference<>();

        public void single(Message message) {
            lastMessage.set(message);
        }

        public CompletableFuture<Void> multiple(Message message, RuntimeContext context, Update update) {
            lastMessage.set(message);
            lastContext.set(context);
            lastUpdate.set(update);
            return CompletableFuture.completedFuture(null);
        }

        public void stringOnly(String value) {
            lastString.set(value);
        }

        public void unsupported(Integer ignored) {
        }
    }

    private static Update messageUpdate(String text) {
        return new Update(
                new UpdateId("u-inv-1"),
                UpdateType.MESSAGE,
                message(text),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Message message(String text) {
        return new Message(
                new MessageId("m-inv-1"),
                new Chat(new ChatId("c-inv-1"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("u-inv-user"), "demo", "Demo", "User", "Demo User", false, "en"),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }
}
