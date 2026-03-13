package ru.max.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

class WizardManagerTest {

    @Test
    void enterWizardSetsFirstStep() {
        Fixture fixture = fixture();

        fixture.wizardManager.enter("checkout").toCompletableFuture().join();

        WizardStep step = fixture.wizardManager.currentStep().toCompletableFuture().join().orElseThrow();
        assertEquals("email", step.id());
        assertEquals("checkout", fixture.sceneManager.currentScene().toCompletableFuture().join().orElseThrow().sceneId());
    }

    @Test
    void nextAndBackProgressStepWithinBoundaries() {
        Fixture fixture = fixture();
        fixture.wizardManager.enter("checkout").toCompletableFuture().join();

        fixture.wizardManager.next().toCompletableFuture().join();
        assertEquals("confirm", fixture.wizardManager.currentStep().toCompletableFuture().join().orElseThrow().id());

        fixture.wizardManager.next().toCompletableFuture().join();
        assertEquals("done", fixture.wizardManager.currentStep().toCompletableFuture().join().orElseThrow().id());

        fixture.wizardManager.next().toCompletableFuture().join();
        assertEquals("done", fixture.wizardManager.currentStep().toCompletableFuture().join().orElseThrow().id());

        fixture.wizardManager.back().toCompletableFuture().join();
        assertEquals("confirm", fixture.wizardManager.currentStep().toCompletableFuture().join().orElseThrow().id());

        fixture.wizardManager.back().toCompletableFuture().join();
        fixture.wizardManager.back().toCompletableFuture().join();
        assertEquals("email", fixture.wizardManager.currentStep().toCompletableFuture().join().orElseThrow().id());
    }

    @Test
    void exitWizardClearsCurrentSceneAndStepMetadata() {
        Fixture fixture = fixture();
        fixture.wizardManager.enter("checkout").toCompletableFuture().join();
        fixture.wizardManager.next().toCompletableFuture().join();

        fixture.wizardManager.exit().toCompletableFuture().join();

        assertTrue(fixture.wizardManager.currentStep().toCompletableFuture().join().isEmpty());
        assertTrue(fixture.sceneManager.currentScene().toCompletableFuture().join().isEmpty());
        assertTrue(fixture.fsm.data().toCompletableFuture().join().get("wizard.stepIndex").isEmpty());
    }

    @Test
    void boundaryBehaviorFailsWhenWizardNotActive() {
        Fixture fixture = fixture();

        CompletionException nextFailure = assertThrows(
                CompletionException.class,
                () -> fixture.wizardManager.next().toCompletableFuture().join()
        );
        CompletionException backFailure = assertThrows(
                CompletionException.class,
                () -> fixture.wizardManager.back().toCompletableFuture().join()
        );

        assertTrue(nextFailure.getCause() instanceof WizardFlowException);
        assertTrue(backFailure.getCause() instanceof WizardFlowException);
    }

    @Test
    void enterFailsForUnknownWizardId() {
        Fixture fixture = fixture();

        SceneNotFoundException failure = assertThrows(
                SceneNotFoundException.class,
                () -> fixture.wizardManager.enter("missing").toCompletableFuture().join()
        );
        assertEquals("Scene 'missing' is not registered", failure.getMessage());
    }

    @Test
    void enterFailsWhenSceneIsNotWizard() {
        MemoryStorage storage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(storage, StateKey.userInChat(new UserId("u-x"), new ChatId("c-x")));
        SceneRegistry registry = new InMemorySceneRegistry().register(new Scene() {
            @Override
            public String id() {
                return "plain-scene";
            }
        });
        SceneManager sceneManager = new DefaultSceneManager(registry, new MemorySceneStorage(), fsm);
        WizardManager wizardManager = new DefaultWizardManager(registry, sceneManager, fsm);

        WizardFlowException failure = assertThrows(
                WizardFlowException.class,
                () -> wizardManager.enter("plain-scene").toCompletableFuture().join()
        );
        assertTrue(failure.getMessage().contains("is not a wizard"));
    }

    @Test
    void storageFailuresAreWrappedIntoFsmStorageException() {
        FailingStorage storage = new FailingStorage();
        FSMContext fsm = FSMContext.of(storage, StateKey.userInChat(new UserId("u-err"), new ChatId("c-err")));
        SceneRegistry registry = new InMemorySceneRegistry()
                .register(Wizard.named("checkout").step("email").step("confirm").build());
        SceneManager sceneManager = new DefaultSceneManager(registry, new MemorySceneStorage(), fsm);
        WizardManager wizardManager = new DefaultWizardManager(registry, sceneManager, fsm);

        wizardManager.enter("checkout").toCompletableFuture().join();
        storage.failReads = true;
        CompletionException failure = assertThrows(
                CompletionException.class,
                () -> wizardManager.next().toCompletableFuture().join()
        );
        assertTrue(failure.getCause() instanceof FsmStorageException);
        assertTrue(failure.getCause().getMessage().contains("fsm.data"));
    }

    private static Fixture fixture() {
        MemoryStorage storage = new MemoryStorage();
        FSMContext fsm = FSMContext.of(storage, StateKey.userInChat(new UserId("u-1"), new ChatId("c-1")));

        SceneRegistry registry = new InMemorySceneRegistry()
                .register(Wizard.named("checkout")
                        .step("email")
                        .step("confirm")
                        .step("done")
                        .build());

        SceneManager sceneManager = new DefaultSceneManager(registry, new MemorySceneStorage(), fsm);
        WizardManager wizardManager = new DefaultWizardManager(registry, sceneManager, fsm);
        return new Fixture(fsm, sceneManager, wizardManager);
    }

    private record Fixture(FSMContext fsm, SceneManager sceneManager, WizardManager wizardManager) {
    }

    private static final class FailingStorage implements FSMStorage {
        private final MemoryStorage delegate = new MemoryStorage();
        private volatile boolean failReads;

        @Override
        public java.util.concurrent.CompletionStage<java.util.Optional<String>> getState(StateKey key) {
            return delegate.getState(key);
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> setState(StateKey key, String state) {
            return delegate.setState(key, state);
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> clearState(StateKey key) {
            return delegate.clearState(key);
        }

        @Override
        public java.util.concurrent.CompletionStage<StateData> getStateData(StateKey key) {
            if (!failReads) {
                return delegate.getStateData(key);
            }
            return CompletableFuture.failedFuture(new IllegalStateException("storage read failed"));
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> setStateData(StateKey key, StateData data) {
            return delegate.setStateData(key, data);
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> clearStateData(StateKey key) {
            return delegate.clearStateData(key);
        }
    }
}
