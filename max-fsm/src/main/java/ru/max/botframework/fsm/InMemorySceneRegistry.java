package ru.max.botframework.fsm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory scene registry preserving deterministic registration order.
 */
public final class InMemorySceneRegistry implements SceneRegistry {
    private final Map<String, Scene> scenes = new LinkedHashMap<>();

    @Override
    public synchronized SceneRegistry register(Scene scene) {
        Scene candidate = Objects.requireNonNull(scene, "scene");
        String id = normalizeSceneId(candidate.id());
        if (scenes.containsKey(id)) {
            throw new IllegalStateException("scene '%s' is already registered".formatted(id));
        }
        scenes.put(id, candidate);
        return this;
    }

    @Override
    public synchronized Optional<Scene> find(String sceneId) {
        return Optional.ofNullable(scenes.get(normalizeSceneId(sceneId)));
    }

    @Override
    public synchronized Collection<Scene> all() {
        return java.util.List.copyOf(scenes.values());
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
