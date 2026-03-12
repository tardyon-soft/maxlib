package ru.max.botframework.testkit;

import ru.max.botframework.dispatcher.Dispatcher;

/**
 * Minimal entry point for test support API.
 */
public final class DispatcherTestKit {
    private DispatcherTestKit() {
    }

    public static Dispatcher dispatcher() {
        return new Dispatcher();
    }
}
