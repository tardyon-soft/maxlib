package ru.max.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertThrows(IllegalStateException.class, () -> fixture.wizardManager.next().toCompletableFuture().join());
        assertThrows(IllegalStateException.class, () -> fixture.wizardManager.back().toCompletableFuture().join());
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
}
