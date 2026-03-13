package ru.max.botframework.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.max.botframework.dispatcher.DispatchResult;
import ru.max.botframework.dispatcher.DispatchStatus;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.fsm.MemoryStorage;
import ru.max.botframework.fsm.StateScope;
import ru.max.botframework.ingestion.UpdateHandlingResult;
import ru.max.botframework.message.Messages;

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
        assertEquals("/messages", probe.sideEffects().getFirst().path());
        assertFalse(probe.sideEffects().getFirst().body().isEmpty());
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

        assertSame(dispatcher, kit.dispatcherRef());
        assertSame(client, kit.botClient());
    }
}
