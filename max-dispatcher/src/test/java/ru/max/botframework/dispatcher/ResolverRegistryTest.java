package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

class ResolverRegistryTest {

    @Test
    void resolvesUsingFirstSupportingResolver() throws Exception {
        ResolverRegistry registry = new ResolverRegistry()
                .register((parameter, context) -> parameter.type() == String.class
                        ? HandlerParameterResolution.resolved("first")
                        : HandlerParameterResolution.unsupported())
                .register((parameter, context) -> parameter.type() == String.class
                        ? HandlerParameterResolution.resolved("second")
                        : HandlerParameterResolution.unsupported());

        Optional<Object> resolved = registry.resolve(
                descriptor(String.class),
                new HandlerInvocationContext(sampleUpdate().message(), new RuntimeContext(sampleUpdate()))
        );

        assertTrue(resolved.isPresent());
        assertEquals("first", resolved.orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoResolverSupportsParameter() throws Exception {
        ResolverRegistry registry = new ResolverRegistry()
                .register((parameter, context) -> HandlerParameterResolution.unsupported());

        Optional<Object> resolved = registry.resolve(
                descriptor(String.class),
                new HandlerInvocationContext(sampleUpdate().message(), new RuntimeContext(sampleUpdate()))
        );

        assertTrue(resolved.isEmpty());
    }

    @Test
    void wrapsResolverThrownExceptionAsResolverExecutionException() throws Exception {
        ResolverRegistry registry = new ResolverRegistry()
                .register((parameter, context) -> {
                    throw new IllegalStateException("resolver boom");
                });

        ResolverExecutionException thrown = assertThrows(
                ResolverExecutionException.class,
                () -> registry.resolve(
                        descriptor(String.class),
                        new HandlerInvocationContext(sampleUpdate().message(), new RuntimeContext(sampleUpdate()))
                )
        );

        assertTrue(thrown.getCause() instanceof IllegalStateException);
        assertEquals("resolver boom", thrown.getCause().getMessage());
    }

    @Test
    void propagatesKnownParameterResolutionExceptionWithoutWrapping() throws Exception {
        ResolverRegistry registry = new ResolverRegistry()
                .register((parameter, context) -> {
                    throw ParameterResolutionException.resolverFailure(
                            ResolverTarget.class.getDeclaredMethod("handler", String.class),
                            parameter,
                            "TestResolver",
                            new RuntimeException("failure")
                    );
                });

        ParameterResolutionException thrown = assertThrows(
                ParameterResolutionException.class,
                () -> registry.resolve(
                        descriptor(String.class),
                        new HandlerInvocationContext(sampleUpdate().message(), new RuntimeContext(sampleUpdate()))
                )
        );

        assertEquals(ParameterResolutionException.Reason.RESOLVER_FAILURE, thrown.reason());
    }

    private static HandlerParameterDescriptor descriptor(Class<?> type) throws Exception {
        Method method = ResolverTarget.class.getDeclaredMethod("handler", type);
        return new HandlerParameterDescriptor(0, method.getParameters()[0]);
    }

    private static final class ResolverTarget {
        @SuppressWarnings("unused")
        public void handler(String value) {
        }
    }

    private static Update sampleUpdate() {
        return new Update(
                new UpdateId("u-reg-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-reg-1"),
                        new Chat(new ChatId("c-reg-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-reg-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
