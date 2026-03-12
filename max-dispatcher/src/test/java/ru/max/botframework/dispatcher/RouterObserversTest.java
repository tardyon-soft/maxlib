package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.Update;

class RouterObserversTest {

    @Test
    void routerCreatesMvpObservers() {
        Router router = new Router("main");

        assertEquals(ObserverType.UPDATE, router.updates().type());
        assertEquals(ObserverType.MESSAGE, router.messages().type());
        assertEquals(ObserverType.CALLBACK, router.callbacks().type());
        assertEquals(ObserverType.ERROR, router.errors().type());
    }

    @Test
    void registrationApiRegistersHandlersIntoMatchingObservers() {
        Router router = new Router("main");
        EventHandler<Update> updateHandler = event -> CompletableFuture.completedFuture(null);
        EventHandler<Message> messageHandler = event -> CompletableFuture.completedFuture(null);
        EventHandler<Callback> callbackHandler = event -> CompletableFuture.completedFuture(null);
        EventHandler<ErrorEvent> errorHandler = event -> CompletableFuture.completedFuture(null);

        Router returned = router
                .update(updateHandler)
                .message(messageHandler)
                .callback(callbackHandler)
                .error(errorHandler);

        assertSame(router, returned);
        assertEquals(1, router.updates().handlers().size());
        assertSame(updateHandler, router.updates().handlers().getFirst());
        assertEquals(1, router.messages().handlers().size());
        assertSame(messageHandler, router.messages().handlers().getFirst());
        assertEquals(1, router.callbacks().handlers().size());
        assertSame(callbackHandler, router.callbacks().handlers().getFirst());
        assertEquals(1, router.errors().handlers().size());
        assertSame(errorHandler, router.errors().handlers().getFirst());
    }
}
