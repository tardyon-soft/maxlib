package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RouterObserversTest {

    @Test
    void routerCreatesMvpObservers() {
        Router router = new Router("main");

        assertEquals(ObserverType.UPDATE, router.updates().type());
        assertEquals(ObserverType.MESSAGE, router.messages().type());
        assertEquals(ObserverType.CALLBACK, router.callbacks().type());
        assertEquals(ObserverType.ERROR, router.errors().type());
    }
}

