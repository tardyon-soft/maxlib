package ru.tardyon.botframework.fsm;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Default scene manager backed by {@link SceneRegistry}, {@link SceneStorage} and {@link FSMContext}.
 */
public final class DefaultSceneManager implements SceneManager {

    private final SceneRegistry registry;
    private final SceneStorage sceneStorage;
    private final FSMContext fsm;
    private final SceneStateBinding stateBinding;
    private final Clock clock;

    public DefaultSceneManager(SceneRegistry registry, SceneStorage sceneStorage, FSMContext fsm) {
        this(registry, sceneStorage, fsm, SceneStateBinding.prefixed("scene:"), Clock.systemUTC());
    }

    public DefaultSceneManager(
            SceneRegistry registry,
            SceneStorage sceneStorage,
            FSMContext fsm,
            SceneStateBinding stateBinding,
            Clock clock
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.sceneStorage = Objects.requireNonNull(sceneStorage, "sceneStorage");
        this.fsm = Objects.requireNonNull(fsm, "fsm");
        this.stateBinding = Objects.requireNonNull(stateBinding, "stateBinding");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public CompletionStage<Optional<SceneSession>> currentScene() {
        return wrapStorageFailure(sceneStorage.get(fsm.scope()), "sceneStorage.get");
    }

    @Override
    public CompletionStage<Void> enter(String sceneId) {
        String normalized = normalizeSceneId(sceneId);
        Scene scene = registry.find(normalized).orElseThrow(() -> new SceneNotFoundException(normalized));
        SceneSession session = new SceneSession(normalized, clock.instant());
        SceneContext context = new DefaultSceneContext(fsm, session);

        return wrapStorageFailure(sceneStorage.set(fsm.scope(), session), "sceneStorage.set")
                .thenCompose(ignored -> wrapStorageFailure(
                        fsm.setState(stateBinding.stateFor(normalized)),
                        "fsm.setState"
                ))
                .thenCompose(ignored -> scene.onEnter(context));
    }

    @Override
    public CompletionStage<Void> exit() {
        return currentScene().thenCompose(current -> {
            if (current.isEmpty()) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            SceneSession session = current.orElseThrow();
            SceneContext context = new DefaultSceneContext(fsm, session);

            CompletionStage<Void> lifecycle = registry.find(session.sceneId())
                    .map(scene -> scene.onExit(context))
                    .orElseGet(() -> java.util.concurrent.CompletableFuture.completedFuture(null));

            return lifecycle
                    .thenCompose(ignored -> wrapStorageFailure(sceneStorage.clear(fsm.scope()), "sceneStorage.clear"))
                    .thenCompose(ignored -> wrapStorageFailure(fsm.clearState(), "fsm.clearState"));
        });
    }

    private static <T> CompletionStage<T> wrapStorageFailure(CompletionStage<T> stage, String operation) {
        return stage.handle((value, throwable) -> {
            if (throwable == null) {
                return CompletableFuture.completedFuture(value);
            }
            Throwable cause = unwrap(throwable);
            return CompletableFuture.<T>failedFuture(new FsmStorageException(operation, cause));
        }).thenCompose(next -> next);
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
    }

    private static String normalizeSceneId(String sceneId) {
        Objects.requireNonNull(sceneId, "sceneId");
        String normalized = sceneId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("sceneId must not be blank");
        }
        return normalized;
    }
}
