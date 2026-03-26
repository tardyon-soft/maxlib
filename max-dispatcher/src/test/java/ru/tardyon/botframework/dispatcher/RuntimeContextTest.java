package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.SceneStateBinding;
import ru.tardyon.botframework.fsm.StateKeyStrategies;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.fsm.Wizard;
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
        assertThrows(IllegalStateException.class, context::media);
    }

    @Test
    void runtimeFsmHelperRequiresFsmBootstrap() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());

        assertThrows(IllegalStateException.class, context::fsm);
        assertThrows(IllegalStateException.class, () -> context.fsm("screen"));
    }

    @Test
    void runtimeFsmHelperReturnsContextWhenConfigured() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        FSMRuntimeSupport.bootstrap(context, new MemoryStorage(), StateKeyStrategies.forScope(StateScope.USER_IN_CHAT));

        assertEquals("u-ctx-user", context.fsm().scope().userId().value());
        assertEquals("c-ctx-1", context.fsm().scope().chatId().value());
    }

    @Test
    void runtimeFsmNamespacedContextIsIsolatedFromDefaultScope() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        FSMRuntimeSupport.bootstrap(context, new MemoryStorage(), StateKeyStrategies.forScope(StateScope.USER_IN_CHAT));

        context.fsm().setState("user.form").toCompletableFuture().join();
        context.fsm("screen").setState("screen.home").toCompletableFuture().join();

        assertEquals("user.form", context.fsm().currentState().toCompletableFuture().join().orElseThrow());
        assertEquals("screen.home", context.fsm("screen").currentState().toCompletableFuture().join().orElseThrow());
        assertTrue(context.fsm("screen").scope().userId().value().startsWith("screen::"));
        assertTrue(context.fsm("screen").scope().chatId().value().startsWith("screen::"));
    }

    @Test
    void runtimeSceneHelpersRequireSceneBootstrap() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        FSMRuntimeSupport.bootstrap(context, new MemoryStorage(), StateKeyStrategies.forScope(StateScope.USER_IN_CHAT));

        assertThrows(IllegalStateException.class, context::scenes);
        assertThrows(IllegalStateException.class, context::wizard);
    }

    @Test
    void runtimeSceneHelpersReturnManagersWhenConfigured() {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        FSMRuntimeSupport.bootstrap(context, new MemoryStorage(), StateKeyStrategies.forScope(StateScope.USER_IN_CHAT));
        SceneRuntimeSupport.bootstrap(
                context,
                new InMemorySceneRegistry().register(Wizard.named("checkout").step("email").build()),
                new MemorySceneStorage(),
                SceneStateBinding.prefixed("scene:")
        );

        assertTrue(context.scenes().currentScene().toCompletableFuture().join().isEmpty());
        assertTrue(context.wizard().currentStep().toCompletableFuture().join().isEmpty());
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
