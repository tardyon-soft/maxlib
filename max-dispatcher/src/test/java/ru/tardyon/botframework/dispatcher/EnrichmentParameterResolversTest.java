package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
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

class EnrichmentParameterResolversTest {

    @Test
    void resolvesFilterProducedDataByType() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate("pay:42"));
        context.mergeFilterEnrichment(java.util.Map.of("textSuffix", "42"));
        HandlerParameterDescriptor parameter = descriptor(String.class);

        HandlerParameterResolution result = new FilterDataParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(context.update().message(), context));

        assertTrue(result.supported());
        assertEquals("42", result.value());
    }

    @Test
    void resolvesMiddlewareProducedDataByType() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate("hello"));
        context.putEnrichment("traceId", "trace-1");
        HandlerParameterDescriptor parameter = descriptor(String.class);

        HandlerParameterResolution result = new MiddlewareDataParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(context.update().message(), context));

        assertTrue(result.supported());
        assertEquals("trace-1", result.value());
    }

    @Test
    void filterDataHasPriorityOverMiddlewareDataInDefaultRegistry() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate("pay:99"));
        context.mergeFilterEnrichment(java.util.Map.of("filterValue", "from-filter"));
        context.putEnrichment("middlewareValue", "from-middleware");
        HandlerParameterDescriptor parameter = descriptor(String.class);

        ResolverRegistry registry = new ResolverRegistry()
                .register(new FilterDataParameterResolver())
                .register(new MiddlewareDataParameterResolver());

        Object value = registry.resolve(parameter, new HandlerInvocationContext(context.update().message(), context))
                .orElseThrow();

        assertEquals("from-filter", value);
    }

    @Test
    void ambiguousFilterDataProducesConflict() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate("pay:42"));
        context.mergeFilterEnrichment(java.util.Map.of(
                "a", "first",
                "b", "second"
        ));
        HandlerParameterDescriptor parameter = descriptor(String.class);

        assertThrows(
                IllegalStateException.class,
                () -> new FilterDataParameterResolver()
                        .resolve(parameter, new HandlerInvocationContext(context.update().message(), context))
        );
    }

    @Test
    void noDataReturnsUnsupported() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate("hello"));
        HandlerParameterDescriptor parameter = descriptor(String.class);

        HandlerParameterResolution result = new MiddlewareDataParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(context.update().message(), context));

        assertFalse(result.supported());
    }

    private static HandlerParameterDescriptor descriptor(Class<?> type) throws Exception {
        Method method = Target.class.getDeclaredMethod("handler", type);
        return new HandlerParameterDescriptor(0, method.getParameters()[0]);
    }

    private static final class Target {
        @SuppressWarnings("unused")
        public void handler(String value) {
        }
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-enrich-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-enrich-1"),
                        new Chat(new ChatId("c-enrich-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-enrich-user"), "demo", "Demo", "User", "Demo User", false, "en"),
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
