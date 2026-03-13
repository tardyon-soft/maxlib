package ru.max.botframework.fsm;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
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
        return sceneStorage.get(fsm.scope());
    }

    @Override
    public CompletionStage<Void> enter(String sceneId) {
        String normalized = normalizeSceneId(sceneId);
        Scene scene = registry.find(normalized).orElseThrow(() -> new SceneNotFoundException(normalized));
        SceneSession session = new SceneSession(normalized, clock.instant());
        SceneContext context = new DefaultSceneContext(fsm, session);

        return sceneStorage.set(fsm.scope(), session)
                .thenCompose(ignored -> fsm.setState(stateBinding.stateFor(normalized)))
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
                    .thenCompose(ignored -> sceneStorage.clear(fsm.scope()))
                    .thenCompose(ignored -> fsm.clearState());
        });
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
