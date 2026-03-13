package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.fsm.DefaultSceneManager;
import ru.max.botframework.fsm.DefaultWizardManager;
import ru.max.botframework.fsm.FSMContext;
import ru.max.botframework.fsm.SceneManager;
import ru.max.botframework.fsm.SceneRegistry;
import ru.max.botframework.fsm.SceneStateBinding;
import ru.max.botframework.fsm.SceneStorage;
import ru.max.botframework.fsm.WizardManager;

final class SceneRuntimeSupport {
    static final RuntimeDataKey<SceneRegistry> SCENE_REGISTRY_KEY =
            RuntimeDataKey.framework("runtime.scene.registry", SceneRegistry.class);
    static final RuntimeDataKey<SceneStorage> SCENE_STORAGE_KEY =
            RuntimeDataKey.framework("runtime.scene.storage", SceneStorage.class);
    static final RuntimeDataKey<SceneStateBinding> SCENE_STATE_BINDING_KEY =
            RuntimeDataKey.framework("runtime.scene.state-binding", SceneStateBinding.class);

    private SceneRuntimeSupport() {
    }

    static void bootstrap(
            RuntimeContext context,
            SceneRegistry sceneRegistry,
            SceneStorage sceneStorage,
            SceneStateBinding stateBinding
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(stateBinding, "stateBinding");
        if (sceneRegistry == null || sceneStorage == null) {
            return;
        }
        context.putData(SCENE_REGISTRY_KEY, sceneRegistry);
        context.putData(SCENE_STORAGE_KEY, sceneStorage);
        context.putData(SCENE_STATE_BINDING_KEY, stateBinding);
    }

    static Optional<SceneManager> resolveSceneManager(RuntimeContext context) {
        Objects.requireNonNull(context, "context");
        Optional<SceneRegistry> registry = context.dataValue(SCENE_REGISTRY_KEY);
        Optional<SceneStorage> storage = context.dataValue(SCENE_STORAGE_KEY);
        Optional<SceneStateBinding> binding = context.dataValue(SCENE_STATE_BINDING_KEY);
        Optional<FSMContext> fsm = FSMRuntimeSupport.resolve(context);
        if (registry.isEmpty() || storage.isEmpty() || binding.isEmpty() || fsm.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DefaultSceneManager(
                registry.orElseThrow(),
                storage.orElseThrow(),
                fsm.orElseThrow(),
                binding.orElseThrow(),
                java.time.Clock.systemUTC()
        ));
    }

    static Optional<WizardManager> resolveWizardManager(RuntimeContext context) {
        Objects.requireNonNull(context, "context");
        Optional<SceneRegistry> registry = context.dataValue(SCENE_REGISTRY_KEY);
        Optional<SceneManager> sceneManager = resolveSceneManager(context);
        Optional<FSMContext> fsm = FSMRuntimeSupport.resolve(context);
        if (registry.isEmpty() || sceneManager.isEmpty() || fsm.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DefaultWizardManager(
                registry.orElseThrow(),
                sceneManager.orElseThrow(),
                fsm.orElseThrow()
        ));
    }
}
