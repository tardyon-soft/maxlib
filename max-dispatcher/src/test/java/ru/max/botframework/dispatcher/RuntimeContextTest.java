package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
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

class RuntimeContextTest {

    @Test
    void putEnrichmentWithTypedKeyStoresAndReadsTypedValue() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        ContextKey<String> traceId = ContextKey.of("traceId", String.class);

        context.putEnrichment(traceId, "trace-123");

        assertEquals("trace-123", context.enrichmentValue(traceId).orElse(null));
    }

    @Test
    void mergeFilterEnrichmentMergesDataWithoutConflict() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());

        context.putEnrichment("request.id", "req-1");
        context.mergeFilterEnrichment(java.util.Map.of("filter.command", "start"));

        assertEquals("req-1", context.enrichmentValue("request.id", String.class).orElse(null));
        assertEquals("start", context.enrichmentValue("filter.command", String.class).orElse(null));
    }

    @Test
    void mergeFilterEnrichmentRejectsConflictingValues() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        context.putEnrichment("route.id", "outer");

        assertThrows(
                EnrichmentConflictException.class,
                () -> context.mergeFilterEnrichment(java.util.Map.of("route.id", "filter"))
        );
    }

    @Test
    void typedReadFailsForIncompatibleValueType() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        context.putEnrichment("attempts", 1);

        assertThrows(
                IllegalStateException.class,
                () -> context.enrichmentValue("attempts", String.class)
        );
        assertTrue(context.enrichmentValue("attempts", Integer.class).isPresent());
    }

    @Test
    void runtimeDataContainerSupportsTypedPutAndGet() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        RuntimeDataKey<String> appService = RuntimeDataKey.application("serviceName", String.class);

        context.putData(appService, "billing");

        assertEquals("billing", context.dataValue(appService).orElse(null));
    }

    @Test
    void runtimeDataContainerReturnsEmptyWhenValueAbsent() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        RuntimeDataKey<String> missing = RuntimeDataKey.application("missing", String.class);

        assertTrue(context.dataValue(missing).isEmpty());
    }

    @Test
    void runtimeDataContainerSupportsExplicitReplaceRule() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        RuntimeDataKey<String> trace = RuntimeDataKey.middleware("trace", String.class);
        context.putData(trace, "trace-1");

        context.replaceData(trace, "trace-2");

        assertEquals("trace-2", context.dataValue(trace).orElse(null));
    }

    @Test
    void runtimeDataContainerDetectsConflicts() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        RuntimeDataKey<String> key = RuntimeDataKey.framework("framework.id", String.class);
        context.putData(key, "v1");

        RuntimeDataConflictException conflict = assertThrows(
                RuntimeDataConflictException.class,
                () -> context.putData(key, "v2")
        );
        assertEquals("framework.id", conflict.keyName());
    }

    @Test
    void runtimeDataContainerTypedLookupFailsOnTypeMismatch() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        context.putData(RuntimeDataKey.application("retries", Integer.class), 3);

        assertThrows(
                IllegalStateException.class,
                () -> context.data().find("retries", String.class)
        );
    }

    @Test
    void runtimeMessagingHelpersRequireBotClientBootstrap() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());

        assertThrows(IllegalStateException.class, context::messaging);
        assertThrows(IllegalStateException.class, context::callbacks);
        assertThrows(IllegalStateException.class, context::actions);
    }

    private static Update sampleUpdate() {
        return new Update(
                new UpdateId("u-ctx-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-ctx-1"),
                        new Chat(new ChatId("c-ctx-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-ctx-user"), "demo", "Demo", "User", "Demo User", false, "en"),
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
