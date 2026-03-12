package ru.max.botframework.dispatcher;

import java.util.Objects;

/**
 * Logical routing unit that groups handlers by domain.
 */
public final class Router {
    private final String name;

    public Router(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() {
        return name;
    }
}
