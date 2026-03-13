package ru.tardyon.botframework.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.ingestion.UpdateHandlingResult;
import ru.tardyon.botframework.message.Messages;

class DispatcherTestKitTest {

    @Test
    void feedDispatchesUpdateThroughRealRuntime() {
        AtomicReference<String> textRef = new AtomicReference<>();
        Router router = new Router("runtime");
        router.message((message, context) -> {
            textRef.set(message.text());
            return CompletableFuture.completedFuture(null);
        });

        DispatcherTestKit kit = DispatcherTestKit.builder()
                .includeRouter(router)
                .build();

        DispatchResult result = kit.feed(TestUpdates.message("hello"));

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("hello", textRef.get());
    }

    @Test
    void feedAndCaptureCollectsMessageSideEffects() {
        Router router = new Router("reply");
        router.message((message, context) -> {
            context.reply(Messages.text("pong"));
            return CompletableFuture.completedFuture(null);
        });

        DispatcherTestKit kit = DispatcherTestKit.builder()
                .includeRouter(router)
                .build();

        DispatcherTestKit.DispatchProbe probe = kit.feedAndCapture(TestUpdates.message("ping"));

        assertEquals(DispatchStatus.HANDLED, probe.result().status());
        assertEquals(1, probe.sideEffects().size());
        assertTrue(probe.hasCall("/messages"));
        assertEquals(1, probe.callsTo("/messages").size());
        assertFalse(probe.callsTo("/messages").getFirst().body().isEmpty());
    }

    @Test
    void handleUsesIngestionContract() {
        Router router = new Router("handle");
        router.message((message, context) -> CompletableFuture.completedFuture(null));

        DispatcherTestKit kit = DispatcherTestKit.builder()
                .includeRouter(router)
                .build();

        UpdateHandlingResult result = kit.handle(TestUpdates.message("ok"));

        assertTrue(result.success());
        assertTrue(result.error().isEmpty());
    }

    @Test
    void builderAppliesRuntimeCustomizations() {
        MemoryStorage storage = new MemoryStorage();
        Router router = new Router("fsm");
        AtomicReference<StateScope> scopeSeen = new AtomicReference<>();
        router.message((message, fsm) -> {
            scopeSeen.set(fsm.scope().scope());
            return CompletableFuture.completedFuture(null);
        });

        DispatcherTestKit kit = DispatcherTestKit.builder()
                .fsmStorage(storage)
                .stateScope(StateScope.CHAT)
                .includeRouter(router)
                .build();

        DispatchResult result = kit.feed(TestUpdates.message("u-42", "c-42", "state"));

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(StateScope.CHAT, scopeSeen.get());
    }

    @Test
    void customDispatcherAndClientAreReused() {
        Dispatcher dispatcher = new Dispatcher();
        RecordingMaxBotClient client = new RecordingMaxBotClient();

        DispatcherTestKit kit = DispatcherTestKit.builder()
                .dispatcher(dispatcher)
                .botClient(client)
                .build();

        assertSame(dispatcher, kit.runtime());
        assertSame(dispatcher, kit.dispatcherRef());
        assertSame(client, kit.botClient());
    }

    @Test
    void withRouterShortcutCreatesHarness() {
        Router router = new Router("shortcut");
        router.message((message, context) -> CompletableFuture.completedFuture(null));

        DispatcherTestKit kit = DispatcherTestKit.withRouter(router);
        DispatchResult result = kit.feed(TestUpdates.message("hi"));

        assertEquals(DispatchStatus.HANDLED, result.status());
    }

    @Test
    void feedAllSupportsStatefulFlow() {
        MemoryStorage storage = new MemoryStorage();
        Router router = new Router("stateful");
        AtomicReference<String> state = new AtomicReference<>();
        router.message((message, fsm) -> {
            if (fsm.currentState().toCompletableFuture().join().isEmpty()) {
                fsm.setState("form.email").toCompletableFuture().join();
            } else {
                fsm.setState("form.done").toCompletableFuture().join();
            }
            state.set(fsm.currentState().toCompletableFuture().join().orElse(null));
            return CompletableFuture.completedFuture(null);
        });

        DispatcherTestKit kit = DispatcherTestKit.builder()
                .fsmStorage(storage)
                .includeRouter(router)
                .build();

        var results = kit.feedAll(UpdateFixtures.statefulMessages("u-1", "c-1", "start", "next"));

        assertEquals(2, results.size());
        assertEquals(DispatchStatus.HANDLED, results.getFirst().status());
        assertEquals(DispatchStatus.HANDLED, results.get(1).status());
        assertEquals("form.done", state.get());
    }
}
