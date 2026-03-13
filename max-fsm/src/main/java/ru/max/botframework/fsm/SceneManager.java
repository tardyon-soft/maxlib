package ru.max.botframework.fsm;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Scene lifecycle manager for one FSM scope.
 */
public interface SceneManager {

    CompletionStage<Optional<SceneSession>> currentScene();

    CompletionStage<Void> enter(String sceneId);

    CompletionStage<Void> exit();

    default CompletionStage<Void> transition(String sceneId) {
        return exit().thenCompose(ignored -> enter(sceneId));
    }
}
