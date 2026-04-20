package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.Update;

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
        assertSame(updateHandler, router.updates().handlers().get(0));
        assertEquals(1, router.messages().handlers().size());
        assertSame(messageHandler, router.messages().handlers().get(0));
        assertEquals(1, router.callbacks().handlers().size());
        assertSame(callbackHandler, router.callbacks().handlers().get(0));
        assertEquals(1, router.errors().handlers().size());
        assertSame(errorHandler, router.errors().handlers().get(0));
    }

    @Test
    void registrationApiSupportsFilterAwareHandlers() {
        Router router = new Router("main");
        EventHandler<Message> messageHandler = event -> CompletableFuture.completedFuture(null);

        Router returned = router.message(Filter.of(message -> "ok".equals(message.text())), messageHandler);

        assertSame(router, returned);
        assertEquals(1, router.messages().handlers().size());
        assertSame(messageHandler, router.messages().handlers().get(0));
    }

    @Test
    void registrationApiSupportsContextualHandlers() {
        Router router = new Router("main");
        ContextualEventHandler<Message> messageHandler = (message, ctx) -> CompletableFuture.completedFuture(null);

        Router returned = router.message(messageHandler);

        assertSame(router, returned);
        assertEquals(1, router.messages().handlers().size());
        assertSame(messageHandler, router.messages().handlers().get(0));
    }

    @Test
    void routerSupportsInnerMiddlewareRegistration() {
        Router router = new Router("main");
        InnerMiddleware middleware = (ctx, next) -> next.proceed();

        Router returned = router.innerMiddleware(middleware);

        assertSame(router, returned);
        assertEquals(1, router.innerMiddlewares().size());
        assertSame(middleware, router.innerMiddlewares().get(0));
    }

    @Test
    void registrationApiSupportsReflectiveHandlers() throws Exception {
        Router router = new Router("main");
        Target target = new Target();
        Method method = Target.class.getDeclaredMethod("onMessage", Message.class);

        Router returned = router.message(target, method);

        assertSame(router, returned);
        assertEquals(1, router.messages().handlers().size());
    }

    private static final class Target {
        public CompletableFuture<Void> onMessage(Message message) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
