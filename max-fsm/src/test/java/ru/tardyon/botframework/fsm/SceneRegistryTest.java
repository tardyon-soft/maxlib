package ru.tardyon.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class SceneRegistryTest {

    @Test
    void registersAndFindsSceneById() {
        SceneRegistry registry = new InMemorySceneRegistry();
        Scene scene = scene("checkout");

        registry.register(scene);

        assertTrue(registry.find("checkout").isPresent());
        assertEquals(scene, registry.find("checkout").orElseThrow());
        assertEquals(1, registry.all().size());
    }

    @Test
    void rejectsDuplicateSceneRegistration() {
        SceneRegistry registry = new InMemorySceneRegistry();
        registry.register(scene("checkout"));

        assertThrows(IllegalStateException.class, () -> registry.register(scene("checkout")));
    }

    @Test
    void rejectsBlankSceneIdOnLookup() {
        SceneRegistry registry = new InMemorySceneRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.find("  "));
    }

    private static Scene scene(String id) {
        return new Scene() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public java.util.concurrent.CompletionStage<Void> onEnter(SceneContext context) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
