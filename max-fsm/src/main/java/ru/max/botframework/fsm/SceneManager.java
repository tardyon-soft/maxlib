package ru.max.botframework.fsm;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Scene lifecycle manager for one FSM scope.
 *
 * <p>Lifecycle is intentionally simple: {@code enter -> current/transition -> exit}.
 * Unknown scene ids must fail fast (for example with {@link SceneNotFoundException}).</p>
 */
public interface SceneManager {

    /**
     * Returns current scene session or empty when no scene is active.
     */
    CompletionStage<Optional<SceneSession>> currentScene();

    /**
     * Activates scene by id.
     */
    CompletionStage<Void> enter(String sceneId);

    /**
     * Exits currently active scene. No-op when scene is absent.
     */
    CompletionStage<Void> exit();

    default CompletionStage<Void> transition(String sceneId) {
        return exit().thenCompose(ignored -> enter(sceneId));
    }
}
