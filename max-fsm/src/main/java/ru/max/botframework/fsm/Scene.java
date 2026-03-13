package ru.max.botframework.fsm;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Named runtime scene abstraction on top of FSM.
 */
public interface Scene {

    String id();

    default CompletionStage<Void> onEnter(SceneContext context) {
        Objects.requireNonNull(context, "context");
        return CompletableFuture.completedFuture(null);
    }

    default CompletionStage<Void> onExit(SceneContext context) {
        Objects.requireNonNull(context, "context");
        return CompletableFuture.completedFuture(null);
    }
}
