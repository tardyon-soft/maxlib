package ru.max.botframework.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Root orchestrator that owns routing graph.
 */
public final class Dispatcher {
    private final List<Router> routers = new ArrayList<>();

    public Dispatcher includeRouter(Router router) {
        routers.add(Objects.requireNonNull(router, "router"));
        return this;
    }

    public List<Router> routers() {
        return Collections.unmodifiableList(routers);
    }
}
