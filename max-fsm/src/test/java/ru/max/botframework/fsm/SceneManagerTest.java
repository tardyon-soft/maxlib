package ru.max.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

class SceneManagerTest {

    @Test
    void enterStoresCurrentSceneAndBindsFsmState() {
        MemoryStorage fsmStorage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(fsmStorage, StateKey.userInChat(new UserId("u-1"), new ChatId("c-1")));
        SceneRegistry registry = new InMemorySceneRegistry().register(new ProbeScene("checkout"));
        SceneStorage sceneStorage = new MemorySceneStorage();
        SceneManager manager = new DefaultSceneManager(
                registry,
                sceneStorage,
                fsm,
                SceneStateBinding.prefixed("scene:"),
                Clock.fixed(Instant.parse("2026-03-13T00:00:00Z"), ZoneOffset.UTC)
        );

        manager.enter("checkout").toCompletableFuture().join();

        SceneSession session = manager.currentScene().toCompletableFuture().join().orElseThrow();
        assertEquals("checkout", session.sceneId());
        assertEquals(Instant.parse("2026-03-13T00:00:00Z"), session.enteredAt());
        assertEquals("scene:checkout", fsm.currentState().toCompletableFuture().join().orElseThrow());
    }

    @Test
    void exitClearsSceneMetadataAndFsmState() {
        MemoryStorage fsmStorage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(fsmStorage, StateKey.user(new UserId("u-2")));
        AtomicInteger enterCalls = new AtomicInteger();
        AtomicInteger exitCalls = new AtomicInteger();
        Scene scene = new Scene() {
            @Override
            public String id() {
                return "profile";
            }

            @Override
            public java.util.concurrent.CompletionStage<Void> onEnter(SceneContext context) {
                enterCalls.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public java.util.concurrent.CompletionStage<Void> onExit(SceneContext context) {
                exitCalls.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };

        SceneManager manager = new DefaultSceneManager(
                new InMemorySceneRegistry().register(scene),
                new MemorySceneStorage(),
                fsm,
                SceneStateBinding.prefixed("scene:"),
                Clock.systemUTC()
        );

        manager.enter("profile").toCompletableFuture().join();
        manager.exit().toCompletableFuture().join();

        assertEquals(1, enterCalls.get());
        assertEquals(1, exitCalls.get());
        assertTrue(manager.currentScene().toCompletableFuture().join().isEmpty());
        assertTrue(fsm.currentState().toCompletableFuture().join().isEmpty());
    }

    @Test
    void transitionExitsCurrentAndEntersNextScene() {
        MemoryStorage fsmStorage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(fsmStorage, StateKey.chat(new ChatId("c-9")));
        AtomicInteger checkoutExit = new AtomicInteger();
        AtomicInteger confirmEnter = new AtomicInteger();

        Scene checkout = new Scene() {
            @Override
            public String id() {
                return "checkout";
            }

            @Override
            public java.util.concurrent.CompletionStage<Void> onExit(SceneContext context) {
                checkoutExit.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };
        Scene confirm = new Scene() {
            @Override
            public String id() {
                return "confirm";
            }

            @Override
            public java.util.concurrent.CompletionStage<Void> onEnter(SceneContext context) {
                confirmEnter.incrementAndGet();
                return context.fsm().updateData(Map.of("step", "confirm")).thenApply(ignored -> null);
            }
        };

        SceneManager manager = new DefaultSceneManager(
                new InMemorySceneRegistry().register(checkout).register(confirm),
                new MemorySceneStorage(),
                fsm
        );

        manager.enter("checkout").toCompletableFuture().join();
        manager.transition("confirm").toCompletableFuture().join();

        assertEquals(1, checkoutExit.get());
        assertEquals(1, confirmEnter.get());
        assertEquals("confirm", manager.currentScene().toCompletableFuture().join().orElseThrow().sceneId());
        assertEquals("scene:confirm", fsm.currentState().toCompletableFuture().join().orElseThrow());
        assertEquals("confirm", fsm.data().toCompletableFuture().join().get("step", String.class).orElseThrow());
    }

    @Test
    void enterFailsForUnknownScene() {
        MemoryStorage fsmStorage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(fsmStorage, StateKey.user(new UserId("u-42")));
        SceneManager manager = new DefaultSceneManager(new InMemorySceneRegistry(), new MemorySceneStorage(), fsm);

        SceneNotFoundException thrown = assertThrows(
                SceneNotFoundException.class,
                () -> manager.enter("missing").toCompletableFuture().join()
        );
        assertEquals("Scene 'missing' is not registered", thrown.getMessage());
    }

    @Test
    void enterRejectsBlankSceneId() {
        MemoryStorage fsmStorage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(fsmStorage, StateKey.user(new UserId("u-43")));
        SceneManager manager = new DefaultSceneManager(new InMemorySceneRegistry(), new MemorySceneStorage(), fsm);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> manager.enter("  ").toCompletableFuture().join()
        );
        assertEquals("sceneId must not be blank", thrown.getMessage());
    }

    @Test
    void sceneStorageFailuresAreWrappedIntoFsmStorageException() {
        MemoryStorage fsmStorage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(fsmStorage, StateKey.user(new UserId("u-99")));
        SceneStorage failingStorage = new SceneStorage() {
            @Override
            public CompletionStage<java.util.Optional<SceneSession>> get(StateKey key) {
                return CompletableFuture.failedFuture(new IllegalStateException("storage unavailable"));
            }

            @Override
            public CompletionStage<Void> set(StateKey key, SceneSession session) {
                return CompletableFuture.failedFuture(new IllegalStateException("storage unavailable"));
            }

            @Override
            public CompletionStage<Void> clear(StateKey key) {
                return CompletableFuture.failedFuture(new IllegalStateException("storage unavailable"));
            }
        };
        SceneManager manager = new DefaultSceneManager(
                new InMemorySceneRegistry().register(new ProbeScene("checkout")),
                failingStorage,
                fsm
        );

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> manager.enter("checkout").toCompletableFuture().join()
        );
        assertTrue(thrown.getCause() instanceof FsmStorageException);
        assertTrue(thrown.getCause().getMessage().contains("sceneStorage.set"));
        assertTrue(thrown.getCause().getCause() instanceof IllegalStateException);
    }

    private static final class ProbeScene implements Scene {
        private final String id;

        private ProbeScene(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
