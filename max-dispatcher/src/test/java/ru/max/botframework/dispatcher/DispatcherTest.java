package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DispatcherTest {

    @Test
    void includeRouterAddsRouterToGraph() {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");

        dispatcher.includeRouter(router);

        assertEquals(1, dispatcher.routers().size());
        assertEquals("main", dispatcher.routers().getFirst().name());
    }
}
